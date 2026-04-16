package com.campus.auction.service;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service layer for Auction business logic.
 * Handles CRUD operations, validation, and lifecycle state transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService {

    private final AuctionRepository auctionRepository;

    /**
     * Create a new auction (initial DRAFT state).
     * Validates auction fields before persisting.
     */
    public AuctionDTO createAuction(AuctionDTO auctionDTO) {
        log.info("Creating new auction: {}", auctionDTO.getTitle());

        // Convert DTO to entity
        Auction auction = Auction.builder()
                .title(auctionDTO.getTitle())
                .description(auctionDTO.getDescription())
                .price(auctionDTO.getPrice())
                .reservePrice(auctionDTO.getReservePrice())
                .imageUrl(auctionDTO.getImageUrl())
                .sellerId(auctionDTO.getSellerId())
                .startTime(auctionDTO.getStartTime())
                .endTime(auctionDTO.getEndTime())
                .status(AuctionStatus.DRAFT)
                .build();

        // Validate before persistence
        auction.validateForCreation();

        // TODO: Call User Service to verify seller is verified
        // TODO: If seller not verified, throw 403 Forbidden

        Auction saved = auctionRepository.save(auction);
        log.info("Auction created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Validate auction fields before scheduling.
     * Returns true if valid, throws exception if invalid.
     */
    public boolean validateAuction(Long auctionId) {
        log.info("Validating auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        try {
            auction.validateForCreation();
            log.info("Auction {} validation passed", auctionId);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("Auction {} validation failed: {}", auctionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Schedule an auction (transition from DRAFT to SCHEDULED).
     * Must pass validation and seller must be verified.
     */
    public AuctionDTO scheduleAuction(Long auctionId) {
        log.info("Scheduling auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT auctions can be scheduled");
        }

        // Validate before scheduling
        auction.validateForCreation();

        // TODO: Verify seller with User Service
        // checkSellerVerified(auction.getSellerId());

        auction.setStatus(AuctionStatus.SCHEDULED);
        auction.setScheduledAt(LocalDateTime.now());

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} scheduled successfully, start time: {}", auctionId, auction.getStartTime());

        return convertToDTO(saved);
    }

    /**
     * Activate an auction (transition from SCHEDULED to ACTIVE).
     * Called by scheduler when start time is reached.
     */
    public AuctionDTO activateAuction(Long auctionId) {
        log.info("Activating auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new IllegalStateException("Only SCHEDULED auctions can be activated");
        }

        if (LocalDateTime.now().isBefore(auction.getStartTime())) {
            throw new IllegalStateException("Auction start time has not been reached yet");
        }

        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setActivatedAt(LocalDateTime.now());

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} is now ACTIVE, accepting bids until: {}", auctionId, auction.getEndTime());

        // TODO: Notify all interested buyers via email

        return convertToDTO(saved);
    }

    /**
     * End an auction (transition from ACTIVE to ENDED).
     * Called by scheduler when end time is reached.
     */
    public AuctionDTO endAuction(Long auctionId) {
        log.info("Ending auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE auctions can be ended");
        }

        if (LocalDateTime.now().isBefore(auction.getEndTime())) {
            throw new IllegalStateException("Auction end time has not been reached yet");
        }

        auction.setStatus(AuctionStatus.ENDED);
        auction.setEndedAt(LocalDateTime.now());

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} has ENDED at: {}", auctionId, auction.getEndedAt());

        // TODO: Retrieve highest bid from Bidding Service
        // TODO: If highest bid exists and >= reserve price → CLOSED_SOLD
        //       else → CLOSED_NO_SALE
        // TODO: Notify seller and winner/losers via email

        return convertToDTO(saved);
    }

    /**
     * Close an auction with SOLD status (winner found, reserve met).
     */
    public AuctionDTO closeSold(Long auctionId, String reason) {
        log.info("Closing auction {} as SOLD", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new IllegalStateException("Only ENDED auctions can be closed");
        }

        auction.setStatus(AuctionStatus.CLOSED_SOLD);
        auction.setClosedAt(LocalDateTime.now());
        auction.setClosureReason(reason);

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} closed as SOLD", auctionId);

        // TODO: Trigger payment processing via Payment Service

        return convertToDTO(saved);
    }

    /**
     * Close an auction with NO_SALE status (no winner or reserve not met).
     */
    public AuctionDTO closeNoSale(Long auctionId, String reason) {
        log.info("Closing auction {} as NO_SALE", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new IllegalStateException("Only ENDED auctions can be closed");
        }

        auction.setStatus(AuctionStatus.CLOSED_NO_SALE);
        auction.setClosedAt(LocalDateTime.now());
        auction.setClosureReason(reason);

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} closed as NO_SALE - {}", auctionId, reason);

        return convertToDTO(saved);
    }

    /**
     * Cancel an auction (can be called from any state except already closed).
     */
    public AuctionDTO cancelAuction(Long auctionId, String reason) {
        log.info("Cancelling auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus().toString().startsWith("CLOSED_")) {
            throw new IllegalStateException("Cannot cancel an already closed auction");
        }

        auction.setStatus(AuctionStatus.CLOSED_CANCELLED);
        auction.setClosedAt(LocalDateTime.now());
        auction.setClosureReason(reason);

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} cancelled", auctionId);

        // TODO: Notify all bidders that auction is cancelled

        return convertToDTO(saved);
    }

    /**
     * Archive auction (for record keeping).
     */
    public void archiveAuction(Long auctionId) {
        log.info("Archiving auction: {}", auctionId);
        // TODO: Implement archiving logic (move to archive table or mark as archived)
    }

    /**
     * Get auction by ID.
     */
    @Transactional(readOnly = true)
    public AuctionDTO getAuctionById(Long auctionId) {
        log.debug("Fetching auction: {}", auctionId);
        Auction auction = getAuctionOrThrow(auctionId);
        return convertToDTO(auction);
    }

    /**
     * Get all auctions with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AuctionDTO> getAllAuctions(Pageable pageable) {
        log.debug("Fetching all auctions");
        return auctionRepository.findAll(pageable).map(this::convertToDTO);
    }

    /**
     * Get auctions by seller.
     */
    @Transactional(readOnly = true)
    public Page<AuctionDTO> getAuctionsBySeller(Long sellerId, Pageable pageable) {
        log.debug("Fetching auctions for seller: {}", sellerId);
        return auctionRepository.findBySellerId(sellerId, pageable).map(this::convertToDTO);
    }

    /**
     * Get active auctions for buyers to browse.
     */
    @Transactional(readOnly = true)
    public Page<AuctionDTO> getActiveAuctions(Pageable pageable) {
        log.debug("Fetching active auctions for buyers");
        return auctionRepository.findActiveAuctions(pageable).map(this::convertToDTO);
    }

    /**
     * Get auctions by status.
     */
    @Transactional(readOnly = true)
    public Page<AuctionDTO> getAuctionsByStatus(AuctionStatus status, Pageable pageable) {
        log.debug("Fetching auctions with status: {}", status);
        return auctionRepository.findByStatus(status, pageable).map(this::convertToDTO);
    }

    /**
     * Update auction (only allowed for DRAFT auctions).
     */
    public AuctionDTO updateAuction(Long auctionId, AuctionDTO auctionDTO) {
        log.info("Updating auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT auctions can be updated");
        }

        auction.setTitle(auctionDTO.getTitle());
        auction.setDescription(auctionDTO.getDescription());
        auction.setPrice(auctionDTO.getPrice());
        auction.setReservePrice(auctionDTO.getReservePrice());
        auction.setImageUrl(auctionDTO.getImageUrl());
        auction.setStartTime(auctionDTO.getStartTime());
        auction.setEndTime(auctionDTO.getEndTime());

        // Validate updated values
        auction.validateForCreation();

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} updated successfully", auctionId);

        return convertToDTO(saved);
    }

    /**
     * Helper method to get auction or throw 404.
     */
    private Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("Auction not found: {}", auctionId);
                    return new IllegalArgumentException("Auction not found: " + auctionId);
                });
    }

    /**
     * Convert Auction entity to DTO.
     */
    private AuctionDTO convertToDTO(Auction auction) {
        return AuctionDTO.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .description(auction.getDescription())
                .price(auction.getPrice())
                .reservePrice(auction.getReservePrice())
                .imageUrl(auction.getImageUrl())
                .sellerId(auction.getSellerId())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .updatedAt(auction.getUpdatedAt())
                .scheduledAt(auction.getScheduledAt())
                .activatedAt(auction.getActivatedAt())
                .endedAt(auction.getEndedAt())
                .closedAt(auction.getClosedAt())
                .closureReason(auction.getClosureReason())
                .build();
    }
}
