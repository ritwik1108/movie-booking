package com.moviebooking.dto.catalog;

import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatDetailDto {
    private Long id; // show seat ID
    private Long seatId;
    private String rowLabel;
    private Integer seatNumber;
    private SeatTier tier;
    private ShowSeatStatus status;
    private BigDecimal price;
}
