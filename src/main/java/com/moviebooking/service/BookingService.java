package com.moviebooking.service;

import com.moviebooking.domain.booking.Booking;
import com.moviebooking.domain.booking.BookingLineItem;
import com.moviebooking.domain.booking.BookingStatus;
import com.moviebooking.domain.discount.DiscountCode;
import com.moviebooking.domain.payment.Payment;
import com.moviebooking.domain.payment.PaymentStatus;
import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.domain.user.User;
import com.moviebooking.dto.booking.BookingConfirmResult;
import com.moviebooking.dto.booking.ConfirmBookingRequest;
import com.moviebooking.exception.BadRequestException;
import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.payment.FakePaymentGateway;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PricingService pricingService;
    private final FakePaymentGateway fakePaymentGateway;
    private final DiscountCodeService discountCodeService;
    private final NotificationOutboxService notificationOutboxService;

    @Transactional
    public SeatHold holdSeats(Long showId, List<Long> seatIds, User user) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("SHOW_NOT_FOUND", "Show not found"));

        // Fix deadlock: Sort seat IDs in ascending order to acquire locks in a consistent sequence
        java.util.List<Long> sortedSeatIds = new java.util.ArrayList<>(seatIds);
        java.util.Collections.sort(sortedSeatIds);

        List<ShowSeat> showSeats = showSeatRepository.findByShowIdAndSeatIdInWithLock(showId, sortedSeatIds);

        if (showSeats.size() != seatIds.size()) {
            throw new BadRequestException("INVALID_SEATS", "Some selected seats do not exist for this show");
        }

        // Validate and apply lazy-expiry inline for held seats
        OffsetDateTime now = OffsetDateTime.now();
        for (ShowSeat showSeat : showSeats) {
            if (showSeat.getStatus() == ShowSeatStatus.BOOKED) {
                throw new ConflictException("SEAT_ALREADY_BOOKED", "Seat is already booked");
            }
            if (showSeat.getStatus() == ShowSeatStatus.HELD) {
                SeatHold activeHold = showSeat.getHeldByHold();
                if (activeHold != null && activeHold.getExpiresAt().isBefore(now)) {
                    // Release the expired hold on this seat
                    showSeat.setStatus(ShowSeatStatus.AVAILABLE);
                    showSeat.setHeldByHold(null);
                } else {
                    throw new ConflictException("SEAT_ALREADY_HELD", "Seat is currently held by another user");
                }
            }
        }

        // Create seat hold
        SeatHold hold = SeatHold.builder()
                .user(user)
                .show(show)
                .status(SeatHoldStatus.ACTIVE)
                .expiresAt(now.plusMinutes(5))
                .build();
        seatHoldRepository.save(hold);

        // Update show seats
        for (ShowSeat showSeat : showSeats) {
            showSeat.setStatus(ShowSeatStatus.HELD);
            showSeat.setHeldByHold(hold);
            showSeatRepository.save(showSeat);
        }

        return hold;
    }

    @Transactional
    public BookingConfirmResult confirmBooking(ConfirmBookingRequest request, User user) {
        SeatHold hold = seatHoldRepository.findById(request.getHoldId())
                .orElseThrow(() -> new NotFoundException("HOLD_NOT_FOUND", "Seat hold not found"));

        if (!hold.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("UNAUTHORIZED_HOLD", "This seat hold does not belong to you");
        }

        if (hold.getStatus() != SeatHoldStatus.ACTIVE) {
            throw new BadRequestException("HOLD_INACTIVE", "Seat hold is not active");
        }

        List<ShowSeat> showSeats = showSeatRepository.findByHeldByHold(hold);

        if (hold.getExpiresAt().isBefore(OffsetDateTime.now())) {
            hold.setStatus(SeatHoldStatus.EXPIRED);
            seatHoldRepository.save(hold);

            for (ShowSeat ss : showSeats) {
                ss.setStatus(ShowSeatStatus.AVAILABLE);
                ss.setHeldByHold(null);
                showSeatRepository.save(ss);
            }
            throw new BadRequestException("HOLD_EXPIRED", "Seat hold has expired");
        }

        // Lock these show seats to ensure exclusive confirmation access
        List<Long> seatIds = showSeats.stream().map(ss -> ss.getSeat().getId()).collect(Collectors.toList());
        showSeatRepository.findByShowIdAndSeatIdInWithLock(hold.getShow().getId(), seatIds);

        // Calculate Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ShowSeat ss : showSeats) {
            BigDecimal seatPrice = pricingService.calculatePrice(hold.getShow(), ss.getSeat().getTier());
            subtotal = subtotal.add(seatPrice);
        }

        // Apply Discount Code if present
        BigDecimal discountAmount = BigDecimal.ZERO;
        DiscountCode discountCode = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().trim().isEmpty()) {
            discountCode = discountCodeService.validateAndLockDiscountCode(request.getDiscountCode(), subtotal);
            if (discountCode.getType() == com.moviebooking.domain.discount.DiscountCodeType.PERCENTAGE) {
                discountAmount = subtotal.multiply(discountCode.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                discountAmount = discountCode.getValue();
            }
            // Cap discount at subtotal
            if (discountAmount.compareTo(subtotal) > 0) {
                discountAmount = subtotal;
            }

            discountCode.setUsesConsumed(discountCode.getUsesConsumed() + 1);
            discountCodeRepository.save(discountCode);
        }

        BigDecimal totalPaid = subtotal.subtract(discountAmount);

        // Save Booking as CONFIRMED initially to get database ID
        Booking booking = Booking.builder()
                .user(user)
                .show(hold.getShow())
                .seatHold(hold)
                .status(BookingStatus.CONFIRMED)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalPaid(totalPaid)
                .discountCode(discountCode)
                .createdAt(OffsetDateTime.now())
                .build();
        bookingRepository.save(booking);

        List<BookingLineItem> lineItems = new ArrayList<>();
        for (ShowSeat ss : showSeats) {
            BigDecimal seatPrice = pricingService.calculatePrice(hold.getShow(), ss.getSeat().getTier());
            lineItems.add(BookingLineItem.builder()
                    .booking(booking)
                    .showSeat(ss)
                    .tier(ss.getSeat().getTier())
                    .priceCharged(seatPrice)
                    .build());
        }
        booking.setLineItems(lineItems);
        bookingRepository.save(booking);

        // Execute payment gateway call
        FakePaymentGateway.PaymentResult paymentResult = fakePaymentGateway.processPayment(request.getPaymentToken(), totalPaid);

        if (paymentResult.success()) {
            // Success flow
            Payment payment = Payment.builder()
                    .booking(booking)
                    .status(PaymentStatus.SUCCESS)
                    .amount(totalPaid)
                    .gatewayReference(paymentResult.gatewayReference())
                    .attemptedAt(OffsetDateTime.now())
                    .build();
            paymentRepository.save(payment);

            hold.setStatus(SeatHoldStatus.CONSUMED);
            seatHoldRepository.save(hold);

            for (ShowSeat ss : showSeats) {
                ss.setStatus(ShowSeatStatus.BOOKED);
                showSeatRepository.save(ss);
            }

            notificationOutboxService.enqueueNotification("BOOKING_CONFIRMED", user, toBookingDto(booking));

            return new BookingConfirmResult(true, booking, null);
        } else {
            // Failure flow: Release seats immediately, update statuses, roll back coupon usages
            Payment payment = Payment.builder()
                    .booking(booking)
                    .status(PaymentStatus.FAILED)
                    .amount(totalPaid)
                    .attemptedAt(OffsetDateTime.now())
                    .build();
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            hold.setStatus(SeatHoldStatus.FAILED);
            seatHoldRepository.save(hold);

            for (ShowSeat ss : showSeats) {
                ss.setStatus(ShowSeatStatus.AVAILABLE);
                ss.setHeldByHold(null);
                showSeatRepository.save(ss);
            }

            if (discountCode != null) {
                discountCode.setUsesConsumed(discountCode.getUsesConsumed() - 1);
                discountCodeRepository.save(discountCode);
            }

            return new BookingConfirmResult(false, booking, paymentResult.message());
        }
    }

    @Transactional(readOnly = true)
    public List<com.moviebooking.dto.booking.BookingDto> getBookingHistory(User user) {
        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::toBookingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.moviebooking.dto.booking.BookingDto getBookingDetails(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("UNAUTHORIZED_BOOKING_ACCESS", "You are not authorized to view this booking");
        }

        return toBookingDto(booking);
    }

    private com.moviebooking.dto.booking.BookingDto toBookingDto(Booking booking) {
        List<String> seatNames = booking.getLineItems().stream()
                .map(li -> li.getShowSeat().getSeat().getRowLabel() + li.getShowSeat().getSeat().getSeatNumber())
                .collect(Collectors.toList());

        return com.moviebooking.dto.booking.BookingDto.builder()
                .id(booking.getId())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .theaterName(booking.getShow().getScreen().getTheater().getName())
                .screenName(booking.getShow().getScreen().getName())
                .seatNames(seatNames)
                .showStartTime(booking.getShow().getStartTime())
                .totalPaid(booking.getTotalPaid())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    @Transactional
    public void releaseHold(Long holdId, User user) {
        // Placeholder
    }
}
