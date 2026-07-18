package com.moviebooking.controller;

import com.moviebooking.dto.catalog.*;
import com.moviebooking.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // --- Cities ---
    @PostMapping("/cities")
    public ResponseEntity<CityDto> createCity(@Valid @RequestBody CityDto dto) {
        return new ResponseEntity<>(catalogService.createCity(dto), HttpStatus.CREATED);
    }

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getAllCities() {
        return ResponseEntity.ok(catalogService.getAllCities());
    }

    @PutMapping("/cities/{id}")
    public ResponseEntity<CityDto> updateCity(@PathVariable Long id, @Valid @RequestBody CityDto dto) {
        return ResponseEntity.ok(catalogService.updateCity(id, dto));
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        catalogService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }

    // --- Theaters ---
    @PostMapping("/theaters")
    public ResponseEntity<TheaterDto> createTheater(@Valid @RequestBody TheaterDto dto) {
        return new ResponseEntity<>(catalogService.createTheater(dto), HttpStatus.CREATED);
    }

    @GetMapping("/theaters")
    public ResponseEntity<List<TheaterDto>> getTheaters(@RequestParam Long cityId) {
        return ResponseEntity.ok(catalogService.getTheatersByCity(cityId));
    }

    @DeleteMapping("/theaters/{id}")
    public ResponseEntity<Void> deleteTheater(@PathVariable Long id) {
        catalogService.deleteTheater(id);
        return ResponseEntity.noContent().build();
    }

    // --- Screens ---
    @PostMapping("/theaters/{theaterId}/screens")
    public ResponseEntity<ScreenDto> createScreen(@PathVariable Long theaterId, @Valid @RequestBody ScreenDto dto) {
        dto.setTheaterId(theaterId);
        return new ResponseEntity<>(catalogService.createScreen(dto), HttpStatus.CREATED);
    }

    @GetMapping("/theaters/{theaterId}/screens")
    public ResponseEntity<List<ScreenDto>> getScreens(@PathVariable Long theaterId) {
        return ResponseEntity.ok(catalogService.getScreensByTheater(theaterId));
    }

    @DeleteMapping("/screens/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable Long id) {
        catalogService.deleteScreen(id);
        return ResponseEntity.noContent().build();
    }

    // --- Seats ---
    @PostMapping("/screens/{screenId}/seats")
    public ResponseEntity<Void> createSeats(@PathVariable Long screenId, @Valid @RequestBody BulkSeatCreationRequest request) {
        catalogService.createSeats(screenId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- Movies ---
    @PostMapping("/movies")
    public ResponseEntity<MovieDto> createMovie(@Valid @RequestBody MovieDto dto) {
        return new ResponseEntity<>(catalogService.createMovie(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        catalogService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shows ---
    @PostMapping("/shows")
    public ResponseEntity<ShowDto> createShow(@Valid @RequestBody ShowDto dto) {
        return new ResponseEntity<>(catalogService.createShow(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        catalogService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }

    // --- Pricing Rules ---
    @PostMapping("/pricing-rules")
    public ResponseEntity<PricingRuleDto> createPricingRule(@Valid @RequestBody PricingRuleDto dto) {
        return new ResponseEntity<>(catalogService.createPricingRule(dto), HttpStatus.CREATED);
    }
}
