package com.campus.auction.service;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that transitions auctions between states
 * based on their start/end times (like a Quartz job).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;

    /**
     * Runs every 30 seconds — activates SCHEDULED auctions whose
     * start time has passed.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void activateScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> toActivate = auctionRepository
                .findByStatusAndStartTimeBefore(AuctionStatus.SCHEDULED, now);

        for (Auction auction : toActivate) {
            auction.setStatus(AuctionStatus.ACTIVE);
            auctionRepository.save(auction);
            log.info("Auction {} activated at {}", auction.getId(), now);
        }
    }

    /**
     * Runs every 30 seconds — ends ACTIVE auctions whose
     * end time has passed.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void endActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> toEnd = auctionRepository
                .findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);

        for (Auction auction : toEnd) {
            auction.setStatus(AuctionStatus.ENDED);

            // Determine sold vs. no-sale based on reserve price
            if (auction.getCurrentHighestBid() != null
                    && auction.getReservePrice() != null
                    && auction.getCurrentHighestBid()
                        .compareTo(auction.getReservePrice()) >= 0) {
                auction.setStatus(AuctionStatus.SOLD);
                log.info("Auction {} SOLD at {}", auction.getId(),
                         auction.getCurrentHighestBid());
            } else if (auction.getCurrentHighestBid() == null
                    || auction.getCurrentHighestBid()
                        .compareTo(java.math.BigDecimal.ZERO) == 0) {
                auction.setStatus(AuctionStatus.CLOSED_NO_SALE);
                log.info("Auction {} closed with no bids", auction.getId());
            } else {
                auction.setStatus(AuctionStatus.CLOSED_NO_SALE);
                log.info("Auction {} closed — reserve not met", auction.getId());
            }

            auctionRepository.save(auction);
        }
    }
}
