package com.campus.auction.pattern;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Design Pattern: Structural - Facade
 * Provides a simplified interface for complex multi-step auction operations.
 * Controllers interact with this facade instead of managing multiple service calls.
 */
@Component
@RequiredArgsConstructor
public class AuctionFacade {

    private final AuctionService auctionService;

    /**
     * Facade method to cancel an auction and trigger necessary side-effects
     * (e.g., in a fully integrated system, notifying bidders or refunding fees).
     */
    @Transactional
    public AuctionDTO cancelAuctionProcess(Long auctionId) {
        // Step 1: Execute core cancellation logic via AuctionService
        AuctionDTO cancelledAuction = auctionService.cancelAuction(auctionId);
        
        // Step 2: (Future Integration) Notify subscribers via Kafka or REST
        // notificationService.notifyAuctionCancelled(auctionId);
        
        return cancelledAuction;
    }

    /**
     * Facade method to finalize a completed auction.
     */
    @Transactional
    public AuctionDTO finalizeAuctionProcess(Long auctionId) {
        // Step 1: Execute core close logic
        AuctionDTO closedAuction = auctionService.closeAuction(auctionId);
        
        // Step 2: (Future Integration) Trigger Payment Service escrow transfer
        // paymentClient.initiateEscrowTransfer(closedAuction);
        
        return closedAuction;
    }
}
