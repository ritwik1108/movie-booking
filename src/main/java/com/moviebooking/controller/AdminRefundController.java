package com.moviebooking.controller;

import com.moviebooking.dto.refund.RefundPolicyDto;
import com.moviebooking.dto.refund.RefundRuleDto;
import com.moviebooking.service.RefundPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/refund-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final RefundPolicyService refundPolicyService;

    @PostMapping
    public ResponseEntity<RefundPolicyDto> createRefundPolicy(@Valid @RequestBody RefundPolicyDto dto) {
        return new ResponseEntity<>(refundPolicyService.createRefundPolicy(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RefundPolicyDto>> getAllRefundPolicies() {
        return ResponseEntity.ok(refundPolicyService.getAllRefundPolicies());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRefundPolicy(@PathVariable Long id) {
        refundPolicyService.deleteRefundPolicy(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rules")
    public ResponseEntity<RefundRuleDto> addRuleToPolicy(
            @PathVariable Long id,
            @Valid @RequestBody RefundRuleDto ruleDto
    ) {
        return new ResponseEntity<>(refundPolicyService.addRuleToPolicy(id, ruleDto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRuleFromPolicy(
            @PathVariable Long id,
            @PathVariable Long ruleId
    ) {
        refundPolicyService.deleteRuleFromPolicy(id, ruleId);
        return ResponseEntity.noContent().build();
    }
}
