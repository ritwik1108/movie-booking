package com.moviebooking.service;

import com.moviebooking.domain.pricing.PricingRule;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.show.Show;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.PricingRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;

    public PricingService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    public BigDecimal calculatePrice(Show show, SeatTier tier) {
        PricingRule rule = resolvePricingRule(show, tier);
        BigDecimal price = rule.getBasePrice();

        if (isWeekend(show.getStartTime())) {
            price = price.multiply(rule.getWeekendMultiplier());
        }

        return price;
    }

    public PricingRule resolvePricingRule(Show show, SeatTier tier) {
        // 1. Check Show Scope
        Optional<PricingRule> showRule = pricingRuleRepository.findByScopeAndScopeRefIdAndSeatTier(
                PricingRuleScope.SHOW, show.getId(), tier
        );
        if (showRule.isPresent()) {
            return showRule.get();
        }

        // 2. Check Theater Scope
        Long theaterId = show.getScreen().getTheater().getId();
        Optional<PricingRule> theaterRule = pricingRuleRepository.findByScopeAndScopeRefIdAndSeatTier(
                PricingRuleScope.THEATER, theaterId, tier
        );
        if (theaterRule.isPresent()) {
            return theaterRule.get();
        }

        // 3. Check Global Scope
        Optional<PricingRule> globalRule = pricingRuleRepository.findByScopeAndSeatTier(
                PricingRuleScope.GLOBAL, tier
        );
        if (globalRule.isPresent()) {
            return globalRule.get();
        }

        // Fallback or throw exception
        throw new NotFoundException("PRICING_RULE_NOT_FOUND", 
                String.format("Pricing rule not found for tier %s in show %d", tier, show.getId()));
    }

    private boolean isWeekend(OffsetDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
