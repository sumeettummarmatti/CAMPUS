package com.campus.payment.service;

import java.math.BigDecimal;

public interface IPaymentGateway {
    /**
     * Attempt to charge the given amount using the specified payment method reference.
     * Returns true if the charge succeeded, false if it failed.
     */
    boolean charge(BigDecimal amount, String paymentMethodRef);

    /**
     * Verify that a previously initiated charge succeeded (idempotency check).
     */
    boolean verify(String gatewayRef);

    /**
     * Refund a previously completed charge.
     */
    boolean refund(String gatewayRef, BigDecimal amount);
}
