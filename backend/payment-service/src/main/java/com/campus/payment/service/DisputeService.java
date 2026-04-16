package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.model.DisputeStatus;
import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import com.campus.payment.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages the dispute lifecycle for in-escrow transactions.
 * Flow: IN_ESCROW or SHIPPED → (open) → DISPUTED[OPEN] → (review) → DISPUTED[UNDER_REVIEW]
 *        → (resolve buyer) → REFUNDED  OR  (resolve seller) → COMPLETED
 */
@Service
public class DisputeService {

    private final TransactionRepository transactionRepository;
    private final TransactionQueryService transactionQueryService;
    private final EscrowService escrowService;
    private final INotificationService notifier;

    public DisputeService(TransactionRepository transactionRepository,
                          TransactionQueryService transactionQueryService,
                          EscrowService escrowService,
                          INotificationService notifier) {
        this.transactionRepository = transactionRepository;
        this.transactionQueryService = transactionQueryService;
        this.escrowService = escrowService;
        this.notifier = notifier;
    }

    /**
     * Open a dispute on an in-escrow or shipped transaction.
     * Transitions: IN_ESCROW or SHIPPED → DISPUTED, dispute status → OPEN.
     */
    @Transactional
    public PaymentDTO openDispute(Long transactionId, String reason) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.IN_ESCROW
                && tx.getStatus() != TransactionStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Disputes can only be opened for IN_ESCROW or SHIPPED transactions. Current: " + tx.getStatus());
        }

        tx.setPreDisputeStatus(tx.getStatus());
        tx.setStatus(TransactionStatus.DISPUTED);
        tx.setDisputeStatus(DisputeStatus.OPEN);
        tx.setDisputeReason(reason);
        notifier.notifyDisputeOpened(tx.getWinnerId(), tx.getSellerId(), tx.getAuctionId());
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Place a dispute under admin review — dispute status: OPEN → UNDER_REVIEW.
     */
    @Transactional
    public PaymentDTO reviewDispute(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getDisputeStatus() != DisputeStatus.OPEN) {
            throw new IllegalStateException("Only OPEN disputes can be placed under review");
        }

        tx.setDisputeStatus(DisputeStatus.UNDER_REVIEW);
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Resolve dispute in buyer's favour — refunds the buyer.
     */
    @Transactional
    public PaymentDTO resolveDisputeBuyer(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);
        requireUnderReview(tx);

        tx.setDisputeStatus(DisputeStatus.RESOLVED_BUYER);
        transactionRepository.save(tx);
        notifier.notifyDisputeResolved(tx.getWinnerId(), tx.getSellerId(), "BUYER");
        return escrowService.refundFunds(transactionId);
    }

    /**
     * Resolve dispute in seller's favour — releases funds to seller.
     * Handles the release directly to avoid intermediate state visibility.
     */
    @Transactional
    public PaymentDTO resolveDisputeSeller(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);
        requireUnderReview(tx);

        tx.setDisputeStatus(DisputeStatus.RESOLVED_SELLER);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setReleasedAt(LocalDateTime.now());
        notifier.notifyDisputeResolved(tx.getWinnerId(), tx.getSellerId(), "SELLER");
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Close a dispute without further action (e.g. withdrawn by buyer).
     * Transaction is restored to the status it held before the dispute was opened;
     * dispute status → CLOSED.
     */
    @Transactional
    public PaymentDTO closeDispute(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getDisputeStatus() == null || tx.getDisputeStatus() == DisputeStatus.CLOSED) {
            throw new IllegalStateException("No active dispute to close on transaction: " + transactionId);
        }

        TransactionStatus restoreStatus = tx.getPreDisputeStatus() != null
                ? tx.getPreDisputeStatus()
                : TransactionStatus.IN_ESCROW;

        tx.setDisputeStatus(DisputeStatus.CLOSED);
        tx.setStatus(restoreStatus);
        tx.setPreDisputeStatus(null);
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    // ── private helper ──────────────────────────────────────────────────────

    private void requireUnderReview(Transaction tx) {
        if (tx.getDisputeStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Dispute must be UNDER_REVIEW to resolve");
        }
    }
}
