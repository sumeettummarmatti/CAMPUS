package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.model.PaymentMethod;
import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import com.campus.payment.repository.TransactionRepository;
import com.campus.payment.service.paymentmode.PaymentProcessor;
import com.campus.payment.service.paymentmode.PaymentProcessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumSet;
import java.util.List;

/**
 * Core payment gateway — initiates, confirms, and cancels transactions.
 */
@Service
public class PaymentGatewayService {

    private final TransactionRepository transactionRepository;
    private final TransactionQueryService transactionQueryService;
    private final EscrowService escrowService;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final PaymentUserSyncService paymentUserSyncService;

    public PaymentGatewayService(TransactionRepository transactionRepository,
                                 TransactionQueryService transactionQueryService,
                                 EscrowService escrowService,
                                 PaymentProcessorFactory paymentProcessorFactory,
                                 PaymentUserSyncService paymentUserSyncService) {
        this.transactionRepository = transactionRepository;
        this.transactionQueryService = transactionQueryService;
        this.escrowService = escrowService;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.paymentUserSyncService = paymentUserSyncService;
    }

    /**
     * Initiate a new payment for a completed auction.
     * Creates a PENDING transaction; call confirmPayment to move it to IN_ESCROW.
     */
    @Transactional
    public PaymentDTO initiatePayment(PaymentRequest request) {
        validateBuyerMode(request.getWinnerId(), request.getPaymentMethod());

        PaymentDTO existing = findExistingAuctionPayment(request.getAuctionId());
        if (existing != null) {
            return existing;
        }

        Transaction tx = Transaction.builder()
                .auctionId(request.getAuctionId())
                .winnerId(request.getWinnerId())
                .sellerId(request.getSellerId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .build();

        Transaction saved = transactionRepository.save(tx);
        paymentUserSyncService.syncTransaction(saved);
        return transactionQueryService.toDTO(saved);
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
        PaymentProcessor processor = paymentProcessorFactory.create(tx.getPaymentMethod());
        boolean success = processor.charge(toRequest(tx));
        if (!success) {
            throw new IllegalStateException("Payment gateway rejected the charge");
        }

        Transaction saved = transactionRepository.save(tx);
        paymentUserSyncService.syncTransaction(saved);
        return transactionQueryService.toDTO(saved);
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
        Transaction saved = transactionRepository.save(tx);
        paymentUserSyncService.syncTransaction(saved);
        return transactionQueryService.toDTO(saved);
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
        Transaction saved = transactionRepository.save(tx);
        paymentUserSyncService.syncTransaction(saved);
        return transactionQueryService.toDTO(saved);
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
        Transaction saved = transactionRepository.save(tx);
        paymentUserSyncService.syncTransaction(saved);
        return transactionQueryService.toDTO(saved);
    }

    /**
     * Full transaction automation after auction enters ENDED/CLOSED_SOLD flow.
     * Transition chain:
     * PENDING -> PAYMENT_PROCESSING -> IN_ESCROW -> SHIPPED -> DELIVERY_CONFIRMED -> COMPLETED
     */
    @Transactional
    public PaymentDTO autoSettlePayment(PaymentRequest request) {
        ensurePreferredMode(request);
        PaymentDTO existing = findExistingAuctionPayment(request.getAuctionId());
        if (existing != null) {
            return existing;
        }
        PaymentDTO initiated = initiatePayment(request);
        PaymentDTO processing = confirmPayment(initiated.getId());
        PaymentDTO inEscrow = escrowService.holdFunds(processing.getId());
        PaymentDTO shipped = escrowService.markShipped(inEscrow.getId());
        PaymentDTO delivered = escrowService.confirmDelivery(shipped.getId());
        return escrowService.releaseFunds(delivered.getId());
    }

    private void validateBuyerMode(Long winnerId, PaymentMethod method) {
        if (!paymentUserSyncService.isModeEnabledForUser(winnerId, method.name())) {
            throw new IllegalStateException("Winner has not enabled payment mode " + method + " in profile wallet");
        }
    }

    private PaymentRequest toRequest(Transaction tx) {
        PaymentRequest request = new PaymentRequest();
        request.setAuctionId(tx.getAuctionId());
        request.setWinnerId(tx.getWinnerId());
        request.setSellerId(tx.getSellerId());
        request.setAmount(tx.getAmount());
        request.setPaymentMethod(tx.getPaymentMethod());
        return request;
    }

    private void ensurePreferredMode(PaymentRequest request) {
        if (request.getPaymentMethod() != null && isEnabled(request.getWinnerId(), request.getPaymentMethod())) {
            return;
        }
        if (request.getPaymentMethod() == null && isEnabled(request.getWinnerId(), PaymentMethod.CAMPUS_WALLET)) {
            request.setPaymentMethod(PaymentMethod.CAMPUS_WALLET);
            return;
        }
        for (String mode : paymentUserSyncService.getEnabledModesForUser(request.getWinnerId())) {
            try {
                request.setPaymentMethod(PaymentMethod.valueOf(mode));
                return;
            } catch (IllegalArgumentException ignored) {
                // keep trying
            }
        }
        throw new IllegalStateException("Winner has no enabled payment mode in profile wallet");
    }

    private boolean isEnabled(Long userId, PaymentMethod method) {
        return paymentUserSyncService.isModeEnabledForUser(userId, method.name());
    }

    private PaymentDTO findExistingAuctionPayment(Long auctionId) {
        List<Transaction> existing = transactionRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);
        if (existing.isEmpty()) {
            return null;
        }
        EnumSet<TransactionStatus> liveStatuses = EnumSet.of(
                TransactionStatus.PENDING,
                TransactionStatus.PAYMENT_PROCESSING,
                TransactionStatus.IN_ESCROW,
                TransactionStatus.SHIPPED,
                TransactionStatus.DELIVERY_CONFIRMED,
                TransactionStatus.COMPLETED,
                TransactionStatus.DISPUTED
        );
        for (Transaction tx : existing) {
            if (liveStatuses.contains(tx.getStatus())) {
                return transactionQueryService.toDTO(tx);
            }
        }
        return null;
    }
}
