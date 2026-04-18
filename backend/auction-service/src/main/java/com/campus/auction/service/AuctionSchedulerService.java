package com.campus.auction.service;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

    @Value("${services.bidding-service-url}")
    private String biddingServiceUrl;
    
    private final RestTemplate restTemplate;

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

                // Ask Bidding Service for the highest bid
                String url = biddingServiceUrl + "/api/bids/auction/" + auction.getId();
                try{
                    // The bidding service returns a list
                    com.fasterxml.jackson.databind.JsonNode[] bids = restTemplate.getForObject(url, com.fasterxml.jackson.databind.JsonNode[].class);

                    if (bids != null && bids.length > 0){
                        double highestBid = bids[0].get("amount").asDouble();
                        if (highestBid >= auction.getReservePrice()) {
                            auctionService.closeSold(auction.getId(), "Winner determined");
                            // Tell binding service to mark winners/losers
                            restTemplate.postForEntity(
                                biddingServiceUrl + "/api/bids/auction/" + auction.getId() + "/resolve", null, Void.class);
                        } else {
                            auctionService.closeNoSale(auction.getId(), "Reserve price not met");
                        }
                    } else {
                        auctionService.closeNoSale(auction.getId(), "No bids received");
                    }
                } catch (Exception ex) {
                    log.warn("Could not reach bidding service for action {}: {}", auction.getId(), ex.getMessage());
                    auctionService.closeNoSale(auction.getId(), "No bids received");
                }      
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
