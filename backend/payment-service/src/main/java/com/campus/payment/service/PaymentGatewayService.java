package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import com.campus.payment.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core payment gateway — initiates, confirms, and cancels transactions.
 */
@Service
public class PaymentGatewayService {

    private final TransactionRepository transactionRepository;
    private final TransactionQueryService transactionQueryService;
    private final IPaymentGateway gateway;

    public PaymentGatewayService(TransactionRepository transactionRepository,
            TransactionQueryService transactionQueryService, IPaymentGateway gateway) {
        this.transactionRepository = transactionRepository;
        this.transactionQueryService = transactionQueryService;
        this.gateway = gateway;
    }

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

        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Confirm a payment — moves it from PENDING → PAYMENT_PROCESSING.
     * Call EscrowService#holdFunds once the gateway confirms success.
     */
    @Transactional
    public PaymentDTO confirmPayment(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm a transaction that is not PENDING. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.PAYMENT_PROCESSING);

        // Use the abstract gateway (SimulatedPaymentGateway will return true)
        boolean success = gateway.charge(tx.getAmount(), tx.getPaymentMethod().name());
        if (!success) {
            throw new IllegalStateException("Payment gateway rejected the charge");
        }

        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Record a gateway failure — moves PAYMENT_PROCESSING → PAYMENT_FAILED.
     * The buyer may retry via retryPayment or abandon via cancelPayment.
     */
    @Transactional
    public PaymentDTO markPaymentFailed(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PAYMENT_PROCESSING) {
            throw new IllegalStateException(
                    "Only PAYMENT_PROCESSING transactions can be marked failed. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.PAYMENT_FAILED);
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Retry a failed payment — moves PAYMENT_FAILED → PAYMENT_PROCESSING.
     */
    @Transactional
    public PaymentDTO retryPayment(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PAYMENT_FAILED) {
            throw new IllegalStateException(
                    "Only PAYMENT_FAILED transactions can be retried. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.PAYMENT_PROCESSING);
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Cancel a PENDING or PAYMENT_FAILED payment.
     */
    @Transactional
    public PaymentDTO cancelPayment(Long transactionId) {
        Transaction tx = transactionQueryService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PENDING
                && tx.getStatus() != TransactionStatus.PAYMENT_FAILED) {
            throw new IllegalStateException(
                    "Only PENDING or PAYMENT_FAILED payments can be cancelled. Current status: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.CANCELLED);
        return transactionQueryService.toDTO(transactionRepository.save(tx));
    }

}
