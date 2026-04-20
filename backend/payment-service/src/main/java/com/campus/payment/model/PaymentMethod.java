package com.campus.payment.model;

/**
 * Supported payment methods on the CAMPUS platform.
 */
public enum PaymentMethod {
    CAMPUS_WALLET,
    GOOGLE_PAY,
    CARD,
    CASH,
    NET_BANKING,

    // Backward-compatible aliases for older stored values
    CASH_ON_DELIVERY,
    UPI,
    CREDIT_CARD
}
