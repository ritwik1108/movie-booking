package com.moviebooking.service;

import com.moviebooking.domain.pricing.PricingRule;
import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.dto.catalog.ShowSeatDetailDto;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.SeatHoldRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShowSeatService {

    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final PricingService pricingService;

    public ShowSeatService(
            ShowSeatRepository showSeatRepository,
            ShowRepository showRepository,
            SeatHoldRepository seatHoldRepository,
            PricingService pricingService
    ) {
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.pricingService = pricingService;
    }

    public List<ShowSeatDetailDto> getSeatsForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("SHOW_NOT_FOUND", "Show not found"));

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        OffsetDateTime now = OffsetDateTime.now();

        return showSeats.stream().map(ss -> {
            // Lazy check
            if (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldByHold() != null) {
                SeatHold hold = ss.getHeldByHold();
                if (hold.getStatus() == SeatHoldStatus.ACTIVE && hold.getExpiresAt().isBefore(now)) {
                    // Release seat inline (self-healing)
                    ss.setStatus(ShowSeatStatus.AVAILABLE);
                    ss.setHeldByHold(null);
                    showSeatRepository.save(ss);

                    hold.setStatus(SeatHoldStatus.EXPIRED);
                    seatHoldRepository.save(hold);
                }
            }

            // Calculate price
            BigDecimal price = pricingService.calculatePrice(show, ss.getSeat().getTier());

            return ShowSeatDetailDto.builder()
                    .id(ss.getId())
                    .seatId(ss.getSeat().getId())
                    .rowLabel(ss.getSeat().getRowLabel())
                    .seatNumber(ss.getSeat().getSeatNumber())
                    .tier(ss.getSeat().getTier())
                    .status(ss.getStatus())
                    .price(price)
                    .build();
        }).collect(Collectors.toList());
    }
}
