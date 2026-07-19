package com.moviebooking.dto.refund;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPolicyDto {
    private Long id;

    @NotBlank(message = "Policy name is required")
    private String name;

    private List<RefundRuleDto> rules;
}
