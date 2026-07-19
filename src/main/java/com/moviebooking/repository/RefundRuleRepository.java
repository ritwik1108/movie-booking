package com.moviebooking.repository;

import com.moviebooking.domain.refund.RefundRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRuleRepository extends JpaRepository<RefundRule, Long> {
}
