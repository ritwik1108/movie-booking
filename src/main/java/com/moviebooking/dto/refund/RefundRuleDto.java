package com.moviebooking.dto.refund;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRuleDto {
    private Long id;

    @NotNull(message = "Hours before min is required")
    @Min(value = 0, message = "Hours before min must be at least 0")
    private Integer hoursBeforeMin;

    private Integer hoursBeforeMax;

    @NotNull(message = "Refund percentage is required")
    @Min(value = 0, message = "Refund percentage must be at least 0")
    @Max(value = 100, message = "Refund percentage cannot exceed 100")
    private BigDecimal refundPercentage;
}
