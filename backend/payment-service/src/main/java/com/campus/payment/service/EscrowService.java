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
 * Manages escrow fund lifecycle: hold funds, release to seller, or refund to buyer.
 */
@Service
@RequiredArgsConstructor
public class EscrowService {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayService paymentGatewayService;

    /**
     * Hold funds in escrow — transition PENDING → IN_ESCROW.
     * Delegates to PaymentGatewayService to avoid duplicated logic.
     */
    @Transactional
    public PaymentDTO holdFunds(Long transactionId) {
        return paymentGatewayService.confirmPayment(transactionId);
    }

    /**
     * Release escrowed funds to the seller — transition IN_ESCROW → COMPLETED.
     */
    @Transactional
    public PaymentDTO releaseFunds(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.IN_ESCROW) {
            throw new IllegalStateException(
                    "Funds can only be released from IN_ESCROW status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setReleasedAt(LocalDateTime.now());
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }

    /**
     * Refund escrowed funds to the buyer — transition IN_ESCROW or DISPUTED → REFUNDED.
     */
    @Transactional
    public PaymentDTO refundFunds(Long transactionId) {
        Transaction tx = paymentGatewayService.findOrThrow(transactionId);

        if (tx.getStatus() != TransactionStatus.IN_ESCROW
                && tx.getStatus() != TransactionStatus.DISPUTED) {
            throw new IllegalStateException(
                    "Funds can only be refunded from IN_ESCROW or DISPUTED status. Current: " + tx.getStatus());
        }

        tx.setStatus(TransactionStatus.REFUNDED);
        return paymentGatewayService.toDTO(transactionRepository.save(tx));
    }
}
