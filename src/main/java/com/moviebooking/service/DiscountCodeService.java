package com.moviebooking.service;

import com.moviebooking.domain.discount.DiscountCode;
import com.moviebooking.dto.discount.DiscountCodeDto;
import com.moviebooking.exception.BadRequestException;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;

    public DiscountCodeDto createDiscountCode(DiscountCodeDto dto) {
        if (dto.getValidTo().isBefore(dto.getValidFrom())) {
            throw new BadRequestException("INVALID_COUPON_DATES", "Coupon end date must be after start date");
        }

        DiscountCode dc = DiscountCode.builder()
                .code(dto.getCode().toUpperCase())
                .type(dto.getType())
                .value(dto.getValue())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .maxUses(dto.getMaxUses())
                .usesConsumed(0)
                .minBookingAmount(dto.getMinBookingAmount())
                .build();

        DiscountCode saved = discountCodeRepository.save(dc);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DiscountCodeDto> getAllDiscountCodes() {
        return discountCodeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void deleteDiscountCode(Long id) {
        if (!discountCodeRepository.existsById(id)) {
            throw new NotFoundException("DISCOUNT_CODE_NOT_FOUND", "Discount code not found");
        }
        discountCodeRepository.deleteById(id);
    }

    /**
     * Finds and pessimistically locks a discount code by code, validating that it is active and has remaining uses.
     */
    public DiscountCode validateAndLockDiscountCode(String code, BigDecimal bookingAmount) {
        DiscountCode discountCode = discountCodeRepository.findByCodeWithLock(code.toUpperCase())
                .orElseThrow(() -> new BadRequestException("INVALID_DISCOUNT_CODE", "Invalid discount code"));

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(discountCode.getValidFrom()) || now.isAfter(discountCode.getValidTo())) {
            throw new BadRequestException("DISCOUNT_CODE_EXPIRED", "Discount code is not active or expired");
        }

        if (discountCode.getUsesConsumed() >= discountCode.getMaxUses()) {
            throw new BadRequestException("DISCOUNT_CODE_FULLY_CONSUMED", "Discount code usage limit reached");
        }

        if (discountCode.getMinBookingAmount() != null && bookingAmount.compareTo(discountCode.getMinBookingAmount()) < 0) {
            throw new BadRequestException("MIN_BOOKING_AMOUNT_NOT_MET", 
                    "Minimum booking amount of " + discountCode.getMinBookingAmount() + " not met");
        }

        return discountCode;
    }

    private DiscountCodeDto toDto(DiscountCode dc) {
        return DiscountCodeDto.builder()
                .id(dc.getId())
                .code(dc.getCode())
                .type(dc.getType())
                .value(dc.getValue())
                .validFrom(dc.getValidFrom())
                .validTo(dc.getValidTo())
                .maxUses(dc.getMaxUses())
                .usesConsumed(dc.getUsesConsumed())
                .minBookingAmount(dc.getMinBookingAmount())
                .build();
    }
}
