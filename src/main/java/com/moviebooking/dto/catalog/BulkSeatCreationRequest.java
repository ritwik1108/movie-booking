package com.moviebooking.dto.catalog;

import com.moviebooking.domain.seat.SeatTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkSeatCreationRequest {

    @NotEmpty(message = "Layout rows cannot be empty")
    @Valid
    private List<RowLayout> layouts;

    @Data
    public static class RowLayout {
        @NotBlank(message = "Row label is required")
        private String rowLabel;

        @NotNull(message = "Number of seats is required")
        @Min(value = 1, message = "Number of seats must be at least 1")
        private Integer numSeats;

        @NotNull(message = "Seat tier is required")
        private SeatTier tier;
    }
}
