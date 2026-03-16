package com.campus.payment.service;

import com.campus.payment.model.DisputeStatus;
import com.campus.payment.model.Transaction;
import com.campus.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrow service — holds, releases, and refunds funds.
 * Implements the payment-state.puml lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayService paymentGateway;

    /**
     * Create a new transaction for an auction winner.
     */
    public Transaction createTransaction(Long auctionId, Long buyerId,
                                          Long sellerId, java.math.BigDecimal amount) {
        Transaction txn = Transaction.builder()
                .auctionId(auctionId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status(DisputeStatus.PENDING_PAYMENT)
                .build();

        return transactionRepository.save(txn);
    }

    /**
     * Process payment — charge buyer and hold in escrow.
     * PENDING_PAYMENT → PAYMENT_PROCESSING → IN_ESCROW (or PAYMENT_FAILED)
     */
    @Transactional
    public Transaction processPayment(Long txnId) {
        Transaction txn = findById(txnId);
        assertStatus(txn, DisputeStatus.PENDING_PAYMENT, DisputeStatus.PAYMENT_FAILED);

        txn.setStatus(DisputeStatus.PAYMENT_PROCESSING);
        transactionRepository.save(txn);

        try {
            String ref = paymentGateway.chargePayment(txn.getAmount(), txn.getBuyerId());
            txn.setPaymentReference(ref);
            txn.setStatus(DisputeStatus.IN_ESCROW);
            log.info("Transaction {} — funds held in escrow", txnId);
        } catch (Exception e) {
            txn.setStatus(DisputeStatus.PAYMENT_FAILED);
            log.error("Transaction {} — payment failed: {}", txnId, e.getMessage());
        }

        return transactionRepository.save(txn);
    }

    /**
     * Seller marks item as shipped.
     * IN_ESCROW → SHIPPED
     */
    @Transactional
    public Transaction markShipped(Long txnId, String trackingInfo) {
        Transaction txn = findById(txnId);
        assertStatus(txn, DisputeStatus.IN_ESCROW);

        txn.setStatus(DisputeStatus.SHIPPED);
        txn.setTrackingInfo(trackingInfo);
        log.info("Transaction {} — item shipped, tracking: {}", txnId, trackingInfo);

        return transactionRepository.save(txn);
    }

    /**
     * Buyer confirms delivery — release funds to seller.
     * SHIPPED → DELIVERY_CONFIRMED → COMPLETED
     */
    @Transactional
    public Transaction confirmDelivery(Long txnId) {
        Transaction txn = findById(txnId);
        assertStatus(txn, DisputeStatus.SHIPPED);

        txn.setStatus(DisputeStatus.COMPLETED);
        log.info("Transaction {} — delivery confirmed, funds released to seller", txnId);

        return transactionRepository.save(txn);
    }

    /**
     * Refund buyer — used when dispute is resolved in buyer's favour.
     */
    @Transactional
    public Transaction refund(Long txnId) {
        Transaction txn = findById(txnId);

        paymentGateway.refundPayment(txn.getPaymentReference());
        txn.setStatus(DisputeStatus.REFUNDED);
        log.info("Transaction {} — refunded to buyer", txnId);

        return transactionRepository.save(txn);
    }

    // ── Helpers ──────────────────────────────────────────

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    private void assertStatus(Transaction txn, DisputeStatus... allowed) {
        for (DisputeStatus s : allowed) {
            if (txn.getStatus() == s) return;
        }
        throw new IllegalStateException(
                "Transaction " + txn.getId() + " is in state " + txn.getStatus()
                + " — expected one of: " + java.util.Arrays.toString(allowed));
    }
}
