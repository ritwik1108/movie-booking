package com.moviebooking.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {

    @NotNull(message = "Hold ID is required")
    private Long holdId;

    @NotBlank(message = "Payment token is required")
    private String paymentToken;

    private String discountCode;
}
