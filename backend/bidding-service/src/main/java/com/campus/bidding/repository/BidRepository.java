package com.campus.bidding.repository;

import com.campus.bidding.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for Bid entities.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /** All bids for an auction, ordered by amount descending. */
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    /** The current highest bid on an auction. */
    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(Long auctionId);

    /** All bids placed by a specific bidder. */
    List<Bid> findByBidderId(Long bidderId);

    /** Check if bidder already leads this auction. */
    Optional<Bid> findTopByAuctionIdAndBidderIdOrderByAmountDesc(Long auctionId, Long bidderId);
}
