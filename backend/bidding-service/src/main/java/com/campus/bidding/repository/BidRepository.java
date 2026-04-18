package com.campus.bidding.repository;

import com.campus.bidding.model.Bid;
import com.campus.bidding.model.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    Optional<Bid> findTopByAuctionIdAndStatusOrderByAmountDesc(
        Long auctionId, BidStatus status
    );
    
    List<Bid> findByBuyerIdOrderByPlacedAtDesc(Long buyerId);

    boolean existsByAuctionIdAndBuyerIdAndStatus(
        Long auctionId, Long buyerId, BidStatus status
    );
}