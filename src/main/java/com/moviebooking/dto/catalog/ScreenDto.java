package com.moviebooking.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenDto {
    private Long id;

    @NotNull(message = "Theater ID is required")
    private Long theaterId;

    @NotBlank(message = "Screen name is required")
    private String name;
}
