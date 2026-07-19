package com.moviebooking.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class FakePaymentGateway {

    public PaymentResult processPayment(String paymentToken, BigDecimal amount) {
        if (paymentToken == null || paymentToken.trim().isEmpty()) {
            return new PaymentResult(false, null, "Payment token is missing");
        }

        if (paymentToken.toLowerCase().startsWith("fail")) {
            return new PaymentResult(false, null, "Card declined / Insufficient funds");
        }

        // Return success with a random gateway reference
        String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(true, ref, "SUCCESS");
    }

    public record PaymentResult(boolean success, String gatewayReference, String message) {}
}
