package com.moviebooking.repository;

import com.moviebooking.domain.refund.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
}
