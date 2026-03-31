package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import com.campus.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core payment gateway — initiates, confirms, and cancels transactions.
 */
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final TransactionRepository transactionRepository;

    /**
     * Initiate a new payment for a completed auction.
     * Creates a PENDING transaction; call confirmPayment to move it to IN_ESCROW.
     */
    @Transactional
    public PaymentDTO initiatePayment(PaymentRequest request) {
        // Prevent duplicate payment for the same auction
        transactionRepository.findByAuctionIdAndStatus(
                        request.getAuctionId(), TransactionStatus.PENDING)
                .ifPresent(t -> {
                    throw new IllegalStateException(
                            "A pending payment already exists for auction: " + request.getAuctionId());
                });

        transactionRepository.findByAuctionIdAndStatus(
                        request.getAuctionId(), TransactionStatus.IN_ESCROW)
                .ifPresent(t -> {
                    throw new IllegalStateException(
                            "Payment for auction " + request.getAuctionId() + " is already in escrow");
                });

        Transaction tx = Transaction.builder()
                .auctionId(request.getAuctionId())
                .winnerId(request.getWinnerId())
                .sellerId(request.getSellerId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .build();

        return toDTO(transactionRepository.save(tx));
    }

    /**
     * Confirm a payment — moves it from PENDING → IN_ESCROW.
     */
    @Transactional
    public PaymentDTO confirmPayment(Long transactionId) {
        Transaction tx = findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm a transaction that is not PENDING. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.IN_ESCROW);
        return toDTO(transactionRepository.save(tx));
    }

    /**
     * Cancel a PENDING payment.
     */
    @Transactional
    public PaymentDTO cancelPayment(Long transactionId) {
        Transaction tx = findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING payments can be cancelled. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.CANCELLED);
        return toDTO(transactionRepository.save(tx));
    }

    /**
     * Retrieve a single transaction by ID.
     */
    public PaymentDTO getTransaction(Long transactionId) {
        return toDTO(findOrThrow(transactionId));
    }

    /**
     * All transactions for a given auction.
     */
    public List<PaymentDTO> getTransactionsByAuction(Long auctionId) {
        return transactionRepository.findByAuctionId(auctionId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * All transactions where user is the buyer (winner).
     */
    public List<PaymentDTO> getTransactionsByWinner(Long winnerId) {
        return transactionRepository.findByWinnerId(winnerId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * All transactions where user is the seller.
     */
    public List<PaymentDTO> getTransactionsBySeller(Long sellerId) {
        return transactionRepository.findBySellerId(sellerId)
                .stream().map(this::toDTO).toList();
    }

    // ── Package-private helpers used by Escrow/DisputeService ───────────────

    Transaction findOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    PaymentDTO toDTO(Transaction tx) {
        return PaymentDTO.builder()
                .id(tx.getId())
                .auctionId(tx.getAuctionId())
                .winnerId(tx.getWinnerId())
                .sellerId(tx.getSellerId())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .paymentMethod(tx.getPaymentMethod())
                .disputeStatus(tx.getDisputeStatus())
                .disputeReason(tx.getDisputeReason())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .releasedAt(tx.getReleasedAt())
                .build();
    }
}
