package com.campus.auction.controller;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.pattern.AuctionFacade;
import com.campus.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for auction endpoints (MVC: Controller).
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final AuctionFacade auctionFacade;

    /**
     * POST /api/auctions — create a new auction.
     */
    @PostMapping
    public ResponseEntity<AuctionDTO> create(
            @Valid @RequestBody AuctionDTO dto,
            @RequestHeader("X-User-Id") Long sellerId) {
        AuctionDTO created = auctionService.createAuction(dto, sellerId);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * GET /api/auctions/{id} — get auction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuction(id));
    }

    /**
     * GET /api/auctions — list all auctions.
     */
    @GetMapping
    public ResponseEntity<List<AuctionDTO>> listAll() {
        return ResponseEntity.ok(auctionService.listAll());
    }

    /**
     * GET /api/auctions/active — list only active auctions.
     */
    @GetMapping("/active")
    public ResponseEntity<List<AuctionDTO>> listActive() {
        return ResponseEntity.ok(auctionService.listActive());
    }

    /**
     * GET /api/auctions/seller/{sellerId} — list by seller.
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<AuctionDTO>> listBySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(auctionService.listBySeller(sellerId));
    }

    /**
     * PUT /api/auctions/{id}/close — close an auction manually.
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<AuctionDTO> close(@PathVariable Long id) {
        return ResponseEntity.ok(auctionFacade.finalizeAuctionProcess(id));
    }

    /**
     * PUT /api/auctions/{id}/cancel — cancel an auction.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<AuctionDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(auctionFacade.cancelAuctionProcess(id));
    }

    /**
     * PUT /api/auctions/{id}/bid — internal: update highest bid
     * (called by bidding-service).
     */
    @PutMapping("/{id}/bid")
    public ResponseEntity<Map<String, String>> updateBid(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        auctionService.updateHighestBid(id, amount);
        return ResponseEntity.ok(Map.of("message", "Highest bid updated"));
    }

    /**
     * PUT /api/auctions/{id}/extend — internal: extend end time
     * (anti-snipe, called by bidding-service).
     */
    @PutMapping("/{id}/extend")
    public ResponseEntity<Map<String, String>> extend(
            @PathVariable Long id,
            @RequestParam(defaultValue = "2") int minutes) {
        auctionService.extendEndTime(id, minutes);
        return ResponseEntity.ok(Map.of("message", "Auction extended by " + minutes + " minutes"));
    }
}
