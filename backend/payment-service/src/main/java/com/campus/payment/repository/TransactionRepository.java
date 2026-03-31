package com.campus.payment.repository;

import com.campus.payment.model.Transaction;
import com.campus.payment.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAuctionId(Long auctionId);

    List<Transaction> findByWinnerId(Long winnerId);

    List<Transaction> findBySellerId(Long sellerId);

    Optional<Transaction> findByAuctionIdAndStatus(Long auctionId, TransactionStatus status);
}
