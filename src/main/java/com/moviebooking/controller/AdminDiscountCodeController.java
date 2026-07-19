package com.moviebooking.controller;

import com.moviebooking.dto.discount.DiscountCodeDto;
import com.moviebooking.service.DiscountCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @PostMapping
    public ResponseEntity<DiscountCodeDto> createDiscountCode(@Valid @RequestBody DiscountCodeDto dto) {
        return new ResponseEntity<>(discountCodeService.createDiscountCode(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DiscountCodeDto>> getAllDiscountCodes() {
        return ResponseEntity.ok(discountCodeService.getAllDiscountCodes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscountCode(@PathVariable Long id) {
        discountCodeService.deleteDiscountCode(id);
        return ResponseEntity.noContent().build();
    }
}
