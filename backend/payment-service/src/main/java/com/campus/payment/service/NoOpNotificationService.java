package com.campus.payment.service;

import org.springframework.stereotype.Service;

@Service
public class NoOpNotificationService implements INotificationService {

    @Override
    public void notifyPaymentInitiated(Long winnerId, Long auctionId) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Payment initiated for auction " + auctionId + " by winner " + winnerId);
    }

    @Override
    public void notifyPaymentConfirmed(Long winnerId, Long sellerId, Long auctionId) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Payment confirmed for auction " + auctionId + " by winner " + winnerId + " to seller " + sellerId);
    }

    @Override
    public void notifyFundsReleased(Long sellerId, Long auctionId) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Funds released for auction " + auctionId + " to seller " + sellerId);
    }

    @Override
    public void notifyRefundIssued(Long winnerId, Long auctionId) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Refund issued for auction " + auctionId + " to winner " + winnerId);
    }

    @Override
    public void notifyDisputeOpened(Long winnerId, Long sellerId, Long auctionId) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Dispute opened for auction " + auctionId + " between winner " + winnerId + " and seller " + sellerId);
    }

    @Override
    public void notifyDisputeResolved(Long winnerId, Long sellerId, String ruling) {
        // Replace with EmailService/SMTP call
        System.out.println("[NOTIFICATION] Dispute resolved for winner " + winnerId + " and seller " + sellerId + ". Ruling: " + ruling);
    }
}
