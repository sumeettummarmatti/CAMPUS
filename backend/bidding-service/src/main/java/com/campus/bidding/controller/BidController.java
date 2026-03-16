package com.campus.bidding.controller;

import com.campus.bidding.dto.BidDTO;
import com.campus.bidding.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for bid endpoints (MVC: Controller).
 * Provides HTTP fallback alongside WebSocket for placing bids.
 */
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /**
     * POST /api/bids — place a bid via REST (alternative to WebSocket).
     */
    @PostMapping
    public ResponseEntity<BidDTO> placeBid(
            @Valid @RequestBody BidDTO dto,
            @RequestHeader("X-User-Id") Long bidderId) {
        BidDTO placed = bidService.placeBid(dto, bidderId);
        return ResponseEntity.status(201).body(placed);
    }

    /**
     * GET /api/bids/auction/{auctionId} — all bids for an auction.
     */
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<BidDTO>> listByAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(bidService.getBidsForAuction(auctionId));
    }

    /**
     * GET /api/bids/auction/{auctionId}/highest — current highest bid.
     */
    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<BidDTO> getHighest(@PathVariable Long auctionId) {
        return ResponseEntity.ok(bidService.getHighestBid(auctionId));
    }
}
