package com.moviebooking.scheduler;

import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.repository.SeatHoldRepository;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpirySweeper {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;

    @Scheduled(fixedRateString = "${app.hold.sweeper-interval-ms:30000}")
    @Transactional
    public void sweepExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore(SeatHoldStatus.ACTIVE, now);

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Found {} expired seat holds to release", expiredHolds.size());

        for (SeatHold hold : expiredHolds) {
            try {
                hold.setStatus(SeatHoldStatus.EXPIRED);
                seatHoldRepository.save(hold);

                List<ShowSeat> showSeats = showSeatRepository.findByHeldByHold(hold);
                for (ShowSeat seat : showSeats) {
                    if (seat.getStatus() == ShowSeatStatus.HELD) {
                        seat.setStatus(ShowSeatStatus.AVAILABLE);
                        seat.setHeldByHold(null);
                        showSeatRepository.save(seat);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to release expired seat hold {}", hold.getId(), e);
            }
        }
    }
}
