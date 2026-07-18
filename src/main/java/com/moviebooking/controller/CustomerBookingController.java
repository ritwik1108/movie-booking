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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

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
}
