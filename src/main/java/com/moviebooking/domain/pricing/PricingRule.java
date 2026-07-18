package com.moviebooking.domain.pricing;

import com.moviebooking.domain.seat.SeatTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingRuleScope scope;

    @Column(name = "scope_ref_id")
    private Long scopeRefId;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_tier", nullable = false)
    private SeatTier seatTier;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "weekend_multiplier", nullable = false)
    private BigDecimal weekendMultiplier;
}
