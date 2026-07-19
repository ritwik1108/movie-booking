package com.moviebooking.controller;

import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.user.User;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.booking.HoldResponse;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final com.moviebooking.service.RefundService refundService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @PostMapping("/shows/{showId}/holds")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<HoldResponse> holdSeats(
            @PathVariable Long showId,
            @Valid @RequestBody HoldRequest request
    ) {
        User user = getCurrentUser();
        SeatHold hold = bookingService.holdSeats(showId, request.getSeatIds(), user);
        HoldResponse response = HoldResponse.builder()
                .holdId(hold.getId())
                .expiresAt(hold.getExpiresAt())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/bookings/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<com.moviebooking.dto.booking.ConfirmBookingResponse> confirmBooking(
            @Valid @RequestBody com.moviebooking.dto.booking.ConfirmBookingRequest request
    ) {
        User user = getCurrentUser();
        com.moviebooking.dto.booking.BookingConfirmResult result = bookingService.confirmBooking(request, user);

        if (!result.success()) {
            throw new com.moviebooking.exception.BadRequestException("PAYMENT_FAILED", result.errorMessage());
        }

        com.moviebooking.domain.booking.Booking booking = result.booking();
        List<String> seatNames = booking.getLineItems().stream()
                .map(li -> li.getShowSeat().getSeat().getRowLabel() + li.getShowSeat().getSeat().getSeatNumber())
                .collect(Collectors.toList());

        com.moviebooking.dto.booking.ConfirmBookingResponse response = com.moviebooking.dto.booking.ConfirmBookingResponse.builder()
                .bookingId(booking.getId())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .theaterName(booking.getShow().getScreen().getTheater().getName())
                .screenName(booking.getShow().getScreen().getName())
                .seatNames(seatNames)
                .showStartTime(booking.getShow().getStartTime())
                .subtotal(booking.getSubtotal())
                .discountAmount(booking.getDiscountAmount())
                .totalPaid(booking.getTotalPaid())
                .status(booking.getStatus())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<com.moviebooking.dto.booking.BookingDto>> getBookingHistory() {
        User user = getCurrentUser();
        return ResponseEntity.ok(bookingService.getBookingHistory(user));
    }

    @GetMapping("/bookings/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<com.moviebooking.dto.booking.BookingDto> getBookingDetails(@PathVariable Long id) {
        User user = getCurrentUser();
        return ResponseEntity.ok(bookingService.getBookingDetails(id, user));
    }

    @PostMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<com.moviebooking.dto.booking.CancelBookingResponse> cancelBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        return ResponseEntity.ok(refundService.cancelBooking(id, user));
    }
}
