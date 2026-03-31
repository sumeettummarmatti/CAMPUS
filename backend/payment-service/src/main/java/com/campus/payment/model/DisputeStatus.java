package com.campus.payment.model;

/**
 * Lifecycle states for a payment dispute.
 */
public enum DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED_BUYER,
    RESOLVED_SELLER,
    CLOSED
}
