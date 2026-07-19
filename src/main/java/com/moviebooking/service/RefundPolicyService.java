package com.moviebooking.service;

import com.moviebooking.domain.refund.RefundPolicy;
import com.moviebooking.domain.refund.RefundRule;
import com.moviebooking.dto.refund.RefundPolicyDto;
import com.moviebooking.dto.refund.RefundRuleDto;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.RefundPolicyRepository;
import com.moviebooking.repository.RefundRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final RefundRuleRepository refundRuleRepository;

    public RefundPolicyDto createRefundPolicy(RefundPolicyDto dto) {
        RefundPolicy policy = RefundPolicy.builder()
                .name(dto.getName())
                .build();
        RefundPolicy saved = refundPolicyRepository.save(policy);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RefundPolicyDto> getAllRefundPolicies() {
        return refundPolicyRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void deleteRefundPolicy(Long id) {
        if (!refundPolicyRepository.existsById(id)) {
            throw new NotFoundException("REFUND_POLICY_NOT_FOUND", "Refund policy not found");
        }
        refundPolicyRepository.deleteById(id);
    }

    public RefundRuleDto addRuleToPolicy(Long policyId, RefundRuleDto ruleDto) {
        RefundPolicy policy = refundPolicyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("REFUND_POLICY_NOT_FOUND", "Refund policy not found"));

        RefundRule rule = RefundRule.builder()
                .refundPolicy(policy)
                .hoursBeforeMin(ruleDto.getHoursBeforeMin())
                .hoursBeforeMax(ruleDto.getHoursBeforeMax())
                .refundPercentage(ruleDto.getRefundPercentage())
                .build();

        policy.getRules().add(rule);

        RefundRule saved = refundRuleRepository.save(rule);
        return toRuleDto(saved);
    }

    public void deleteRuleFromPolicy(Long policyId, Long ruleId) {
        RefundRule rule = refundRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("REFUND_RULE_NOT_FOUND", "Refund rule not found"));

        if (!rule.getRefundPolicy().getId().equals(policyId)) {
            throw new NotFoundException("REFUND_RULE_POLICY_MISMATCH", "Refund rule does not belong to this policy");
        }

        rule.getRefundPolicy().getRules().remove(rule);
        refundRuleRepository.delete(rule);
    }

    private RefundPolicyDto toDto(RefundPolicy policy) {
        List<RefundRuleDto> rules = policy.getRules().stream()
                .map(this::toRuleDto)
                .collect(Collectors.toList());

        return RefundPolicyDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .rules(rules)
                .build();
    }

    private RefundRuleDto toRuleDto(RefundRule rule) {
        return RefundRuleDto.builder()
                .id(rule.getId())
                .hoursBeforeMin(rule.getHoursBeforeMin())
                .hoursBeforeMax(rule.getHoursBeforeMax())
                .refundPercentage(rule.getRefundPercentage())
                .build();
    }
}
