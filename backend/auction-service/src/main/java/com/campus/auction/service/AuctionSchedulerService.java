package com.campus.auction.service;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing automated auction lifecycle transitions.
 * Uses scheduled tasks to:
 * - Activate SCHEDULED auctions when start time is reached
 * - End ACTIVE auctions when end time is reached
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
@Transactional
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    /**
     * Run every minute to activate scheduled auctions.
     * Transition: SCHEDULED → ACTIVE
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void activateScheduledAuctions() {
        log.info("Running scheduled task: activating scheduled auctions");

        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctionsToActivate = auctionRepository.findAuctionsToActivate(now);

        if (auctionsToActivate.isEmpty()) {
            log.debug("No auctions to activate at this time");
            return;
        }

        log.info("Found {} auctions to activate", auctionsToActivate.size());

        for (Auction auction : auctionsToActivate) {
            try {
                auctionService.activateAuction(auction.getId());
                log.info("Successfully activated auction ID: {}", auction.getId());
            } catch (Exception e) {
                log.error("Error activating auction ID: {}", auction.getId(), e);
            }
        }
    }

    /**
     * Run every minute to end active auctions.
     * Transition: ACTIVE → ENDED
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void endActiveAuctions() {
        log.info("Running scheduled task: ending active auctions");

        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctionsToEnd = auctionRepository.findAuctionsToEnd(now);

        if (auctionsToEnd.isEmpty()) {
            log.debug("No auctions to end at this time");
            return;
        }

        log.info("Found {} auctions to end", auctionsToEnd.size());

        for (Auction auction : auctionsToEnd) {
            try {
                auctionService.endAuction(auction.getId());
                log.info("Successfully ended auction ID: {}", auction.getId());
                
                // TODO: Query Bidding Service for highest bid
                // TODO: If highest bid >= reserve price → closeSold
                //       else → closeNoSale
                
            } catch (Exception e) {
                log.error("Error ending auction ID: {}", auction.getId(), e);
            }
        }
    }

    /**
     * Optional: Run daily cleanup/archival of closed auctions.
     * Can be scheduled once per day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void archiveClosedAuctions() {
        log.info("Running scheduled task: archiving closed auctions");

        // TODO: Find all auctions closed more than X days ago
        // TODO: Move to archive table or mark as archived
        // TODO: This helps with database performance
    }
}
