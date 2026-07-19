package com.moviebooking.domain.refund;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "refund_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_policy_id", nullable = false)
    @ToString.Exclude
    private RefundPolicy refundPolicy;

    @Column(name = "hours_before_min", nullable = false)
    private Integer hoursBeforeMin;

    @Column(name = "hours_before_max")
    private Integer hoursBeforeMax;

    @Column(name = "refund_percentage", nullable = false)
    private BigDecimal refundPercentage;
}
