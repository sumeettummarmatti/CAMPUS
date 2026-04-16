package com.campus.payment.service;

public interface INotificationService {
    void notifyPaymentInitiated(Long winnerId, Long auctionId);
    void notifyPaymentConfirmed(Long winnerId, Long sellerId, Long auctionId);
    void notifyFundsReleased(Long sellerId, Long auctionId);
    void notifyRefundIssued(Long winnerId, Long auctionId);
    void notifyDisputeOpened(Long winnerId, Long sellerId, Long auctionId);
    void notifyDisputeResolved(Long winnerId, Long sellerId, String ruling);
}
