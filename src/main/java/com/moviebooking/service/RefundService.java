package com.moviebooking.service;

import com.moviebooking.domain.booking.Booking;
import com.moviebooking.domain.booking.BookingLineItem;
import com.moviebooking.domain.booking.BookingStatus;
import com.moviebooking.domain.discount.DiscountCode;
import com.moviebooking.domain.refund.Refund;
import com.moviebooking.domain.refund.RefundPolicy;
import com.moviebooking.domain.refund.RefundRule;
import com.moviebooking.domain.refund.RefundStatus;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.domain.user.User;
import com.moviebooking.dto.booking.CancelBookingResponse;
import com.moviebooking.exception.BadRequestException;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundService {

    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final ShowSeatRepository showSeatRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final NotificationOutboxService notificationOutboxService;

    public CancelBookingResponse cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("UNAUTHORIZED_BOOKING_ACCESS", "You are not authorized to cancel this booking");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("INVALID_BOOKING_STATUS", "Only confirmed bookings can be cancelled");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isAfter(booking.getShow().getStartTime())) {
            throw new BadRequestException("SHOW_ALREADY_STARTED", "Cannot cancel booking for a show that has already started");
        }

        // Calculate Refund
        RefundCalculationResult calcResult = calculateRefund(booking, now);

        // Update Booking Status
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release Show Seats
        List<ShowSeat> showSeats = booking.getLineItems().stream()
                .map(BookingLineItem::getShowSeat)
                .collect(Collectors.toList());

        List<Long> seatIds = showSeats.stream().map(ss -> ss.getSeat().getId()).collect(Collectors.toList());
        showSeatRepository.findByShowIdAndSeatIdInWithLock(booking.getShow().getId(), seatIds);

        for (ShowSeat ss : showSeats) {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHeldByHold(null);
            showSeatRepository.save(ss);
        }

        // Write Refund Record
        Refund refund = Refund.builder()
                .booking(booking)
                .amount(calcResult.amount())
                .status(RefundStatus.PROCESSED)
                .processedAt(now)
                .build();
        refundRepository.save(refund);

        // Restore Coupon Uses if applicable
        DiscountCode discountCode = booking.getDiscountCode();
        if (discountCode != null) {
            discountCode.setUsesConsumed(Math.max(0, discountCode.getUsesConsumed() - 1));
            discountCodeRepository.save(discountCode);
        }

        CancelBookingResponse response = CancelBookingResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .totalPaid(booking.getTotalPaid())
                .refundAmount(calcResult.amount())
                .refundPercentage(calcResult.percentage())
                .build();

        notificationOutboxService.enqueueNotification("BOOKING_CANCELLED", user, response);

        return response;
    }

    private RefundCalculationResult calculateRefund(Booking booking, OffsetDateTime cancellationTime) {
        long hours = Duration.between(cancellationTime, booking.getShow().getStartTime()).toHours();

        List<RefundPolicy> policies = refundPolicyRepository.findAll();
        if (policies.isEmpty()) {
            // Default: 100% refund if no refund policies configured
            BigDecimal hundred = BigDecimal.valueOf(100);
            return new RefundCalculationResult(booking.getTotalPaid(), hundred);
        }

        // Grab first active policy
        RefundPolicy policy = policies.get(0);
        BigDecimal refundPercentage = BigDecimal.ZERO;

        for (RefundRule rule : policy.getRules()) {
            if (hours >= rule.getHoursBeforeMin() && 
                (rule.getHoursBeforeMax() == null || hours <= rule.getHoursBeforeMax())) {
                refundPercentage = rule.getRefundPercentage();
                break;
            }
        }

        BigDecimal refundAmount = booking.getTotalPaid()
                .multiply(refundPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new RefundCalculationResult(refundAmount, refundPercentage);
    }

    private record RefundCalculationResult(BigDecimal amount, BigDecimal percentage) {}
}
