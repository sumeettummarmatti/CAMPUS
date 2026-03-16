package com.campus.auction.model;

/**
 * Auction lifecycle states — matches auction-state.puml.
 */
public enum AuctionStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    ENDED,
    SOLD,
    CLOSED_NO_SALE,
    CANCELLED,
    RELISTED
}
