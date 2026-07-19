package com.moviebooking.dto.discount;

import com.moviebooking.domain.discount.DiscountCodeType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeDto {
    private Long id;

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountCodeType type;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal value;

    @NotNull(message = "Valid from is required")
    private OffsetDateTime validFrom;

    @NotNull(message = "Valid to is required")
    private OffsetDateTime validTo;

    @NotNull(message = "Max uses is required")
    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private Integer usesConsumed;

    @Positive(message = "Minimum booking amount must be positive")
    private BigDecimal minBookingAmount;
}
