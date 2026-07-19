package com.moviebooking.domain.discount;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "discount_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountCodeType type;

    @Column(name = "\"value\"", nullable = false)
    private BigDecimal value;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "uses_consumed", nullable = false)
    private Integer usesConsumed;

    @Column(name = "min_booking_amount")
    private BigDecimal minBookingAmount;
}
