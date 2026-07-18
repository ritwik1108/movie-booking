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
public class TheaterDto {
    private Long id;

    @NotNull(message = "City ID is required")
    private Long cityId;

    @NotBlank(message = "Theater name is required")
    private String name;

    @NotBlank(message = "Theater address is required")
    private String address;
}
