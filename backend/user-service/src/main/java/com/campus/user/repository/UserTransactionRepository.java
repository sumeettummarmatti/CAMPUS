package com.campus.user.repository;

import com.campus.user.model.UserTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {
    Optional<UserTransaction> findByTransactionId(Long transactionId);
    List<UserTransaction> findByWinnerIdOrderByUpdatedAtDesc(Long winnerId);
    List<UserTransaction> findBySellerIdOrderByUpdatedAtDesc(Long sellerId);
}
