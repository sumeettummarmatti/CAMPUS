package com.campus.auction.controller;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * REST Controller for Auction operations.
 * Handles all auction CRUD, state transitions, and lifecycle management.
 * 
 * Base path: /api/auctions
 */
@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    /**
     * Create a new auction (DRAFT state).
     * POST /api/auctions
     */
    @PostMapping
    public ResponseEntity<AuctionDTO> createAuction(@Valid @RequestBody AuctionDTO auctionDTO) {
        log.info("Received request to create auction: {}", auctionDTO.getTitle());
        try {
            AuctionDTO created = auctionService.createAuction(auctionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error creating auction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all auctions with pagination.
     * GET /api/auctions
     */
    @GetMapping
    public ResponseEntity<Page<AuctionDTO>> getAllAuctions(Pageable pageable) {
        log.debug("Fetching all auctions");
        Page<AuctionDTO> auctions = auctionService.getAllAuctions(pageable);
        return ResponseEntity.ok(auctions);
    }

    /**
     * Get active auctions for buyers to browse.
     * GET /api/auctions/browse/active
     */
    @GetMapping("/browse/active")
    public ResponseEntity<Page<AuctionDTO>> getActiveAuctions(Pageable pageable) {
        log.debug("Fetching active auctions for buyer browsing");
        Page<AuctionDTO> auctions = auctionService.getActiveAuctions(pageable);
        return ResponseEntity.ok(auctions);
    }

    /**
     * Get auctions by seller ID.
     * GET /api/auctions/seller/{sellerId}
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<AuctionDTO>> getAuctionsBySeller(
            @PathVariable Long sellerId,
            Pageable pageable) {
        log.debug("Fetching auctions for seller: {}", sellerId);
        Page<AuctionDTO> auctions = auctionService.getAuctionsBySeller(sellerId, pageable);
        return ResponseEntity.ok(auctions);
    }

    /**
     * Get auctions by status.
     * GET /api/auctions/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AuctionDTO>> getAuctionsByStatus(
            @PathVariable AuctionStatus status,
            Pageable pageable) {
        log.debug("Fetching auctions with status: {}", status);
        Page<AuctionDTO> auctions = auctionService.getAuctionsByStatus(status, pageable);
        return ResponseEntity.ok(auctions);
    }

    /**
     * Get auction details by ID.
     * GET /api/auctions/{auctionId}
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionDTO> getAuctionById(@PathVariable Long auctionId) {
        log.debug("Fetching auction: {}", auctionId);
        try {
            AuctionDTO auction = auctionService.getAuctionById(auctionId);
            return ResponseEntity.ok(auction);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update auction (only DRAFT auctions).
     * PUT /api/auctions/{auctionId}
     */
    @PutMapping("/{auctionId}")
    public ResponseEntity<AuctionDTO> updateAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionDTO auctionDTO) {
        log.info("Received request to update auction: {}", auctionId);
        try {
            AuctionDTO updated = auctionService.updateAuction(auctionId, auctionDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found or invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Validate auction details.
     * POST /api/auctions/{auctionId}/validate
     */
    @PostMapping("/{auctionId}/validate")
    public ResponseEntity<String> validateAuction(@PathVariable Long auctionId) {
        log.info("Validating auction: {}", auctionId);
        try {
            auctionService.validateAuction(auctionId);
            return ResponseEntity.ok("Auction validation passed");
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Schedule an auction (DRAFT → SCHEDULED).
     * POST /api/auctions/{auctionId}/schedule
     */
    @PostMapping("/{auctionId}/schedule")
    public ResponseEntity<AuctionDTO> scheduleAuction(@PathVariable Long auctionId) {
        log.info("Scheduling auction: {}", auctionId);
        try {
            AuctionDTO scheduled = auctionService.scheduleAuction(auctionId);
            return ResponseEntity.ok(scheduled);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition or seller not verified: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
            log.error("Error scheduling auction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate an auction (SCHEDULED → ACTIVE).
     * POST /api/auctions/{auctionId}/activate
     * Note: Usually called by scheduler, but exposed for manual testing.
     */
    @PostMapping("/{auctionId}/activate")
    public ResponseEntity<AuctionDTO> activateAuction(@PathVariable Long auctionId) {
        log.info("Activating auction: {}", auctionId);
        try {
            AuctionDTO activated = auctionService.activateAuction(auctionId);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * End an auction (ACTIVE → ENDED).
     * POST /api/auctions/{auctionId}/end
     * Note: Usually called by scheduler, but exposed for manual testing.
     */
    @PostMapping("/{auctionId}/end")
    public ResponseEntity<AuctionDTO> endAuction(@PathVariable Long auctionId) {
        log.info("Ending auction: {}", auctionId);
        try {
            AuctionDTO ended = auctionService.endAuction(auctionId);
            return ResponseEntity.ok(ended);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * Close an auction as SOLD (winner found, reserve met).
     * POST /api/auctions/{auctionId}/close-sold
     */
    @PostMapping("/{auctionId}/close-sold")
    public ResponseEntity<AuctionDTO> closeSold(
            @PathVariable Long auctionId,
            @RequestParam(required = false) String reason) {
        log.info("Closing auction as SOLD: {}", auctionId);
        try {
            AuctionDTO closed = auctionService.closeSold(auctionId, reason != null ? reason : "Winner determined");
            return ResponseEntity.ok(closed);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * Close an auction as NO_SALE (no winner or reserve not met).
     * POST /api/auctions/{auctionId}/close-no-sale
     */
    @PostMapping("/{auctionId}/close-no-sale")
    public ResponseEntity<AuctionDTO> closeNoSale(
            @PathVariable Long auctionId,
            @RequestParam(required = false) String reason) {
        log.info("Closing auction as NO_SALE: {}", auctionId);
        try {
            AuctionDTO closed = auctionService.closeNoSale(auctionId, reason != null ? reason : "No bids received or reserve not met");
            return ResponseEntity.ok(closed);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * Cancel an auction.
     * POST /api/auctions/{auctionId}/cancel
     */
    @PostMapping("/{auctionId}/cancel")
    public ResponseEntity<AuctionDTO> cancelAuction(
            @PathVariable Long auctionId,
            @RequestParam(required = false) String reason) {
        log.info("Cancelling auction: {}", auctionId);
        try {
            AuctionDTO cancelled = auctionService.cancelAuction(auctionId, reason != null ? reason : "Cancelled by seller");
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException e) {
            log.warn("Auction not found: {}", auctionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * Archive auction (record keeping).
     * POST /api/auctions/{auctionId}/archive
     */
    @PostMapping("/{auctionId}/archive")
    public ResponseEntity<Void> archiveAuction(@PathVariable Long auctionId) {
        log.info("Archiving auction: {}", auctionId);
        try {
            auctionService.archiveAuction(auctionId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            log.error("Error archiving auction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint.
     * GET /api/auctions/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auction Service is healthy");
    }
}
