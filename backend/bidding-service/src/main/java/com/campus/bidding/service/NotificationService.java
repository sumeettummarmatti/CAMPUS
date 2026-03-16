package com.campus.bidding.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles notifications to bidders (email, push, etc.).
 * Currently logs notifications — can be extended with
 * email/SMS integration in production.
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Notify a bidder that they have been outbid.
     */
    public void notifyOutbid(Long bidderId, Long auctionId) {
        log.info("NOTIFICATION: User {} has been outbid on auction {}", bidderId, auctionId);
        // TODO: Send email or push notification
    }

    /**
     * Notify the winner when an auction closes.
     */
    public void notifyAuctionWon(Long bidderId, Long auctionId) {
        log.info("NOTIFICATION: User {} won auction {}!", bidderId, auctionId);
        // TODO: Send email or push notification
    }

    /**
     * Notify a bidder that the auction has closed and they lost.
     */
    public void notifyAuctionLost(Long bidderId, Long auctionId) {
        log.info("NOTIFICATION: User {} lost auction {}", bidderId, auctionId);
        // TODO: Send email or push notification
    }
}
