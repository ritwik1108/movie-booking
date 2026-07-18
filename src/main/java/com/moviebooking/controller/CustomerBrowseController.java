package com.moviebooking.controller;

import com.moviebooking.dto.catalog.MovieDto;
import com.moviebooking.dto.catalog.ShowDto;
import com.moviebooking.dto.catalog.ShowSeatDetailDto;
import com.moviebooking.service.CatalogService;
import com.moviebooking.service.ShowSeatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CustomerBrowseController {

    private final CatalogService catalogService;
    private final ShowSeatService showSeatService;

    public CustomerBrowseController(CatalogService catalogService, ShowSeatService showSeatService) {
        this.catalogService = catalogService;
        this.showSeatService = showSeatService;
    }

    @GetMapping("/movies")
    public ResponseEntity<List<MovieDto>> browseMovies(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre
    ) {
        return ResponseEntity.ok(catalogService.searchMovies(cityId, date, language, genre));
    }

    @GetMapping("/movies/{movieId}/shows")
    public ResponseEntity<List<ShowDto>> browseShows(
            @PathVariable Long movieId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date
    ) {
        return ResponseEntity.ok(catalogService.getShowsForMovie(movieId, cityId, date));
    }

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<ShowSeatDetailDto>> getShowSeatMap(@PathVariable Long showId) {
        return ResponseEntity.ok(showSeatService.getSeatsForShow(showId));
    }
}
