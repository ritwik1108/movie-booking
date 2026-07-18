package com.moviebooking.repository;

import com.moviebooking.domain.pricing.PricingRule;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByScopeAndScopeRefIdAndSeatTier(PricingRuleScope scope, Long scopeRefId, SeatTier seatTier);
    Optional<PricingRule> findByScopeAndSeatTier(PricingRuleScope scope, SeatTier seatTier);
}
