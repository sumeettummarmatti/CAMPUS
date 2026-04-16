package com.campus.auction.model;

/**
 * Enum representing all possible states of an Auction.
 * Follows the auction lifecycle: DRAFT → SCHEDULED → ACTIVE → ENDED → CLOSED_*
 */
public enum AuctionStatus {
    DRAFT,                  // Initial state, not yet scheduled
    SCHEDULED,              // Scheduled to start, waiting for start time
    ACTIVE,                 // Currently accepting bids
    ENDED,                  // Bidding period ended, determining winner
    CLOSED_SOLD,            // Auction closed with winner, highest bid ≥ reserve price
    CLOSED_NO_SALE,         // Auction closed with no winner, no bids or highest bid < reserve price
    CLOSED_CANCELLED        // Auction cancelled by seller
}
