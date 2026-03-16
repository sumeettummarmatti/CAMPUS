package com.campus.payment.model;

/**
 * Transaction lifecycle states — matches payment-state.puml.
 * Named DisputeStatus per the original file layout, but covers
 * the full transaction state machine.
 */
public enum DisputeStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    IN_ESCROW,
    SHIPPED,
    DELIVERY_CONFIRMED,
    COMPLETED,
    DISPUTED,
    REFUNDED,
    CANCELLED
}
