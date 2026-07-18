package com.moviebooking.dto.catalog;

import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleDto {
    private Long id;

    @NotNull(message = "Scope is required")
    private PricingRuleScope scope;

    private Long scopeRefId;

    @NotNull(message = "Seat tier is required")
    private SeatTier seatTier;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    @NotNull(message = "Weekend multiplier is required")
    @DecimalMin(value = "1.0", message = "Weekend multiplier must be at least 1.0")
    private BigDecimal weekendMultiplier;
}
