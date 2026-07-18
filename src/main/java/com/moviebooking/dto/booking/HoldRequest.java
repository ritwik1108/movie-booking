package com.moviebooking.dto.booking;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldRequest {
    @NotEmpty(message = "Seat IDs cannot be empty")
    private List<Long> seatIds;
}
