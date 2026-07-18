package com.moviebooking.dto.catalog;

import jakarta.validation.constraints.Min;
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
public class MovieDto {
    private Long id;

    @NotBlank(message = "Movie title is required")
    private String title;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Genre is required")
    private String genre;

    @NotBlank(message = "Certification is required")
    private String certification;
}
