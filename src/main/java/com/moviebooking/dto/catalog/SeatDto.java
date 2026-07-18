package com.moviebooking.dto.catalog;

import com.moviebooking.domain.seat.SeatTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private SeatTier tier;
}
