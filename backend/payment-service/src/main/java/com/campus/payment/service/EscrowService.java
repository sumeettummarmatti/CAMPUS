package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import com.campus.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages escrow fund lifecycle: hold funds, track shipping/delivery, release to seller, or refund to buyer.
 */
@Service
@RequiredArgsConstructor
public class EscrowService {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayService paymentGatewayService;

    /**
     * Hold funds in escrow — transition PAYMENT_PROCESSING → IN_ESCROW.
     * Called once the payment gateway confirms a successful charge.
     */
    @Transactional
    public PaymentDTO holdFunds(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.PAYMENT_PROCESSING) {
            throw new IllegalStateException(
                    "Funds can only be held from PAYMENT_PROCESSING status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.IN_ESCROW);
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Mark the item as shipped by the seller — transition IN_ESCROW → SHIPPED.
     */
    @Transactional
    public PaymentDTO markShipped(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.IN_ESCROW) {
            throw new IllegalStateException(
                    "Item can only be marked shipped from IN_ESCROW status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.SHIPPED);
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Confirm delivery by the buyer (or auto-confirm after 7 days) — transition SHIPPED → DELIVERY_CONFIRMED.
     */
    @Transactional
    public PaymentDTO confirmDelivery(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Delivery can only be confirmed from SHIPPED status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.DELIVERY_CONFIRMED);
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Release escrowed funds to the seller — transition DELIVERY_CONFIRMED → COMPLETED.
     */
    @Transactional
    public PaymentDTO releaseFunds(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.DELIVERY_CONFIRMED) {
            throw new IllegalStateException(
                    "Funds can only be released from DELIVERY_CONFIRMED status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setReleasedAt(LocalDateTime.now());
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Refund escrowed funds to the buyer — transition IN_ESCROW, SHIPPED, or DISPUTED → REFUNDED.
     */
    @Transactional
    public PaymentDTO refundFunds(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.IN_ESCROW
                && tx.getStatus() != TransactionStatus.SHIPPED
                && tx.getStatus() != TransactionStatus.DISPUTED) {
            throw new IllegalStateException(
                    "Funds can only be refunded from IN_ESCROW, SHIPPED, or DISPUTED status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.REFUNDED);
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }
}
