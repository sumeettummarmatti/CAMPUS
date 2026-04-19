package com.campus.auction.service;

import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@Service
@EnableScheduling
@Transactional
public class AuctionSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final RestTemplate restTemplate;

    @Value("${services.bidding-service-url}")
    private String biddingServiceUrl;

    @Value("${services.payment-service-url}")
    private String paymentServiceUrl;

    public AuctionSchedulerService(AuctionRepository auctionRepository,
                                   AuctionService auctionService,
                                   RestTemplate restTemplate) {
        this.auctionRepository = auctionRepository;
        this.auctionService    = auctionService;
        this.restTemplate      = restTemplate;
    }

    /**
     * Run every minute to activate scheduled auctions.
     * Transition: SCHEDULED → ACTIVE
     */
    @Scheduled(fixedRate = 60000)
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
     * Transition: ACTIVE → ENDED → CLOSED_SOLD or CLOSED_NO_SALE
     */
    @Scheduled(fixedRate = 60000)
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
                String bidsUrl = biddingServiceUrl + "/api/bids/auction/" + auction.getId();
                try {
                    com.fasterxml.jackson.databind.JsonNode[] bids =
                        restTemplate.getForObject(bidsUrl, com.fasterxml.jackson.databind.JsonNode[].class);

                    if (bids != null && bids.length > 0) {
                        double highestBid = bids[0].get("amount").asDouble();
                        if (highestBid >= auction.getReservePrice()) {
                            auctionService.closeSold(auction.getId(), "Winner determined");

                            // Tell bidding service to mark winners/losers
                            restTemplate.postForEntity(
                                biddingServiceUrl + "/api/bids/auction/" + auction.getId() + "/resolve",
                                null, Void.class);

                            // Initiate payment for the winner
                            long winnerId = bids[0].get("buyerId").asLong();
                            try {
                                String paymentBody = String.format(
                                    java.util.Locale.US,
                                    "{\"auctionId\":%d,\"winnerId\":%d,\"sellerId\":%d,\"amount\":%.2f,\"paymentMethod\":\"CAMPUS_WALLET\"}",
                                    auction.getId(), winnerId, auction.getSellerId(), highestBid
                                );
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                HttpEntity<String> entity = new HttpEntity<>(paymentBody, headers);
                                restTemplate.postForEntity(
                                    paymentServiceUrl + "/api/payments/initiate",
                                    entity, Void.class
                                );
                                log.info("Payment initiated for auction {}, winner {}, amount {}",
                                    auction.getId(), winnerId, highestBid);
                            } catch (Exception payEx) {
                                log.warn("Could not initiate payment for auction {}: {}",
                                    auction.getId(), payEx.getMessage());
                            }
                        } else {
                            auctionService.closeNoSale(auction.getId(), "Reserve price not met");
                        }
                    } else {
                        auctionService.closeNoSale(auction.getId(), "No bids received");
                    }
                } catch (Exception ex) {
                    log.warn("Could not reach bidding service for auction {}: {}",
                        auction.getId(), ex.getMessage());
                    auctionService.closeNoSale(auction.getId(), "Bidding service unavailable");
                }
            } catch (Exception e) {
                log.error("Error ending auction ID: {}", auction.getId(), e);
            }
        }
    }

    /**
     * Daily cleanup/archival of closed auctions.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void archiveClosedAuctions() {
        log.info("Running scheduled task: archiving closed auctions");
        // TODO: Find all auctions closed more than X days ago and archive them
    }
}
