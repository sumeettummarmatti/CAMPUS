package com.campus.auction.repository;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Auction entity.
 * Provides custom queries for auction lifecycle management and filtering.
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    /**
     * Find all auctions by seller ID.
     */
    Page<Auction> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * Find all auctions with a specific status.
     */
    List<Auction> findByStatus(AuctionStatus status);

    /**
     * Find all auctions by status with pagination.
     */
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    /**
     * Find auctions scheduled to start (status=SCHEDULED with startTime <= now).
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'SCHEDULED' AND a.startTime <= :now")
    List<Auction> findAuctionsToActivate(LocalDateTime now);

    /**
     * Find active auctions that need to end (status=ACTIVE with endTime <= now).
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime <= :now")
    List<Auction> findAuctionsToEnd(LocalDateTime now);

    /**
     * Find auctions created by a seller with a specific status.
     */
    @Query("SELECT a FROM Auction a WHERE a.sellerId = :sellerId AND a.status = :status")
    Page<Auction> findBySellerIdAndStatus(Long sellerId, AuctionStatus status, Pageable pageable);

    /**
     * Find active auctions for buyer browsing.
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' ORDER BY a.startTime DESC")
    Page<Auction> findActiveAuctions(Pageable pageable);
}
