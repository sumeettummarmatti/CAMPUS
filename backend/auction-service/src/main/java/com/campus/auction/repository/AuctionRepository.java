package com.campus.auction.repository;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data-access layer for Auction entities.
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findBySellerId(Long sellerId);

    /** Find auctions that should have started by now but are still SCHEDULED. */
    List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time);

    /** Find auctions that should have ended by now but are still ACTIVE. */
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);
}
