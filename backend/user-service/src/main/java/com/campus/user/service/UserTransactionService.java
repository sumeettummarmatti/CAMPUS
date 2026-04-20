package com.campus.user.service;

import com.campus.user.dto.TransactionSyncRequest;
import com.campus.user.model.User;
import com.campus.user.model.UserTransaction;
import com.campus.user.repository.UserRepository;
import com.campus.user.repository.UserTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserTransactionService {

    private final UserRepository userRepository;
    private final UserTransactionRepository userTransactionRepository;

    public UserTransactionService(UserRepository userRepository, UserTransactionRepository userTransactionRepository) {
        this.userRepository = userRepository;
        this.userTransactionRepository = userTransactionRepository;
    }

    @Transactional
    public void syncTransaction(TransactionSyncRequest request) {
        UserTransaction tx = userTransactionRepository.findByTransactionId(request.getTransactionId())
                .orElseGet(UserTransaction::new);

        tx.setTransactionId(request.getTransactionId());
        tx.setAuctionId(request.getAuctionId());
        tx.setWinnerId(request.getWinnerId());
        tx.setSellerId(request.getSellerId());
        tx.setAmount(request.getAmount());
        tx.setStatus(request.getStatus());
        tx.setPaymentMethod(request.getPaymentMethod());
        userTransactionRepository.save(tx);

        recalculateUserFinancials(request.getWinnerId());
        recalculateUserFinancials(request.getSellerId());
    }

    @Transactional(readOnly = true)
    public List<UserTransaction> buyerTransactions(Long userId) {
        return userTransactionRepository.findByWinnerIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UserTransaction> sellerTransactions(Long userId) {
        return userTransactionRepository.findBySellerIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<String> getEnabledModes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return user.getEnabledPaymentModes();
    }

    @Transactional
    public List<String> updateEnabledModes(Long userId, List<String> modes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setEnabledPaymentModes(modes);
        return userRepository.save(user).getEnabledPaymentModes();
    }

    private void recalculateUserFinancials(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<UserTransaction> buys = userTransactionRepository.findByWinnerIdOrderByUpdatedAtDesc(userId);
        List<UserTransaction> sells = userTransactionRepository.findBySellerIdOrderByUpdatedAtDesc(userId);

        BigDecimal spent = buys.stream()
                .filter(tx -> isBuyerDebit(tx.getStatus()))
                .map(UserTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal earned = sells.stream()
                .filter(tx -> "COMPLETED".equals(tx.getStatus()))
                .map(UserTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        user.setTotalSpent(spent);
        user.setTotalEarned(earned);
        user.setWalletBalance(new BigDecimal("1000.00").add(earned).subtract(spent));
        userRepository.save(user);
    }

    private boolean isBuyerDebit(String status) {
        return switch (status) {
            case "PAYMENT_PROCESSING", "IN_ESCROW", "SHIPPED", "DELIVERY_CONFIRMED", "COMPLETED" -> true;
            default -> false;
        };
    }
}
