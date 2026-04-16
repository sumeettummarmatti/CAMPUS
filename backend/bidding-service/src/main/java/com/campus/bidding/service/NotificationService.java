package com.campus.bidding.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendOutbidNotification(Long buyerId, Long auctionId, Double newHighestBid) {
        System.out.printf(
            "[EMAIL] Buyer %d outbid on auction %d. New highest: %.2f%n",
            buyerId, auctionId, newHighestBid
        );
    }

    public void sendWinNotification(Long buyerId, Long auctionId, Double winningAmount) {
        System.out.printf(
            "[EMAIL] Buyer %d WON auction %d for %.2f%n",
            buyerId, auctionId, winningAmount
        );
    }

    public void sendLossNotification(Long buyerId, Long auctionId) {
        System.out.printf(
            "[EMAIL] Buyer %d did not win auction %d%n",
            buyerId, auctionId
        );
    }

    public void sendBidConfirmation(Long buyerId, Long auctionId, Double amount) {
        System.out.printf(
            "[EMAIL] Bid confirmed — Buyer %d on auction %d for %.2f%n",
            buyerId, auctionId, amount
        );
    }
}