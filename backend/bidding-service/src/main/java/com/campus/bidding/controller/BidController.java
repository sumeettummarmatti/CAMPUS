package com.campus.bidding.controller;

import com.campus.bidding.dto.BidDTO;
import com.campus.bidding.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /** POST /api/bids — place a new bid */
    @PostMapping
    public ResponseEntity<BidDTO> placeBid(@Valid @RequestBody BidDTO dto) {
        return ResponseEntity.status(201).body(bidService.placeBid(dto));
    }

    /** GET /api/bids/auction/{auctionId} — bid history for an auction */
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<BidDTO>> getBidsForAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(bidService.getBidsForAuction(auctionId));
    }

    /** GET /api/bids/buyer/{buyerId} — all bids by a buyer */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<BidDTO>> getBidsByBuyer(@PathVariable Long buyerId) {
        return ResponseEntity.ok(bidService.getBidsByBuyer(buyerId));
    }

    /** POST /api/bids/auction/{auctionId}/resolve — called when auction ends */
    @PostMapping("/auction/{auctionId}/resolve")
    public ResponseEntity<Void> resolveAuction(@PathVariable Long auctionId) {
        bidService.resolveAuctionEnd(auctionId);
        return ResponseEntity.ok().build();
    }
}