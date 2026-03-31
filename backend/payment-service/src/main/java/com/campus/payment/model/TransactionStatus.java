package com.campus.payment.model;

/**
 * Lifecycle states for a payment transaction.
 */
public enum TransactionStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    IN_ESCROW,
    SHIPPED,
    DELIVERY_CONFIRMED,
    COMPLETED,
    REFUNDED,
    DISPUTED,
    CANCELLED
}
