package com.campus.payment.service;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.model.Transaction;
import com.campus.payment.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public PaymentDTO getTransaction(Long transactionId) {
        return toDTO(findOrThrow(transactionId));
    }

    public List<PaymentDTO> getTransactionsByAuction(Long auctionId) {
        return transactionRepository.findByAuctionId(auctionId)
                .stream().map(this::toDTO).toList();
    }

    public List<PaymentDTO> getTransactionsByWinner(Long winnerId) {
        return transactionRepository.findByWinnerId(winnerId)
                .stream().map(this::toDTO).toList();
    }

    public List<PaymentDTO> getTransactionsBySeller(Long sellerId) {
        return transactionRepository.findBySellerId(sellerId)
                .stream().map(this::toDTO).toList();
    }

    public Transaction findOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public PaymentDTO toDTO(Transaction tx) {
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
                .preDisputeStatus(tx.getPreDisputeStatus())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .releasedAt(tx.getReleasedAt())
                .build();
    }
}
