package com.campus.payment.model;

/**
 * Lifecycle states for a payment transaction.
 */
public enum TransactionStatus {
    PENDING,
    IN_ESCROW,
    COMPLETED,
    REFUNDED,
    DISPUTED,
    CANCELLED
}
