package com.moviebooking.service;

import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.domain.user.User;
import com.moviebooking.exception.BadRequestException;
import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Transactional
    public SeatHold holdSeats(Long showId, List<Long> seatIds, User user) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("SHOW_NOT_FOUND", "Show not found"));

        // BUG: We fetch the seats without sorting seatIds list in Java. This leads to deadlocks under concurrency.
        List<ShowSeat> showSeats = showSeatRepository.findByShowIdAndSeatIdInWithLockNoOrder(showId, seatIds);

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
    public void releaseHold(Long holdId, User user) {
        // Placeholder
    }
}
