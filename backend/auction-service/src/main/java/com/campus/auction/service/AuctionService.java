package com.campus.auction.service;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.model.Auction;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Service layer for Auction business logic.
 * Handles CRUD operations, validation, and lifecycle state transitions.
 */
@Service
@Transactional
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionRepository auctionRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${services.bidding-service-url}")
    private String biddingServiceUrl;

    public AuctionService(AuctionRepository auctionRepository, org.springframework.web.client.RestTemplate restTemplate) {
        this.auctionRepository = auctionRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Create a new auction (initial DRAFT state).
     * Validates auction fields before persisting.
     */
    public AuctionDTO createAuction(AuctionDTO auctionDTO) {
        log.info("Creating new auction: {}", auctionDTO.getTitle());

        Auction auction = new Auction();
        auction.setTitle(auctionDTO.getTitle());
        auction.setDescription(auctionDTO.getDescription());
        auction.setPrice(auctionDTO.getPrice());
        auction.setReservePrice(auctionDTO.getReservePrice());
        auction.setImageUrl(auctionDTO.getImageUrl());
        auction.setSellerId(auctionDTO.getSellerId());
        auction.setStartTime(auctionDTO.getStartTime());
        auction.setEndTime(auctionDTO.getEndTime());
        auction.setStatus(AuctionStatus.DRAFT);

        auction.validateForCreation();

        Auction saved = auctionRepository.save(auction);
        log.info("Auction created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Validate auction fields before scheduling.
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
     */
    public AuctionDTO scheduleAuction(Long auctionId) {
        log.info("Scheduling auction: {}", auctionId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT auctions can be scheduled");
        }

        auction.validateForCreation();

        auction.setStatus(AuctionStatus.SCHEDULED);
        auction.setScheduledAt(LocalDateTime.now());

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} scheduled successfully, start time: {}", auctionId, auction.getStartTime());

        // Broadcast to bidding-service
        try {
            String msg = String.format("⏳ NEW AUCTION SCHEDULED: '%s' starting at %s", 
                saved.getTitle(), saved.getStartTime().toString());
            restTemplate.postForObject(biddingServiceUrl + "/api/bids/broadcast", msg, String.class);
        } catch (Exception e) {
            log.error("Failed to broadcast auction scheduling", e);
        }

        return convertToDTO(saved);
    }

    /**
     * Activate an auction (transition from SCHEDULED to ACTIVE).
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

        // Broadcast to bidding-service
        try {
            String msg = String.format("🔥 AUCTION NOW ACTIVE: '%s' (ID: %d)", 
                saved.getTitle(), saved.getId());
            restTemplate.postForObject(biddingServiceUrl + "/api/bids/broadcast", msg, String.class);
        } catch (Exception e) {
            log.error("Failed to broadcast auction activation", e);
        }

        return convertToDTO(saved);
    }

    /**
     * End an auction (transition from ACTIVE to ENDED).
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

        return convertToDTO(saved);
    }

    /**
     * Terminate an auction early (manual ending by seller).
     */
    public AuctionDTO terminateEarly(Long auctionId, Long sellerId) {
        log.info("Terminating auction {} early by seller {}", auctionId, sellerId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE auctions can be terminated early");
        }

        if (!auction.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can terminate this auction");
        }

        auction.setStatus(AuctionStatus.ENDED);
        auction.setEndedAt(LocalDateTime.now());
        auction.setClosureReason("Terminated early by seller");

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} terminated early at: {}", auctionId, auction.getEndedAt());

        return convertToDTO(saved);
    }

    /**
     * Extend an auction end time.
     */
    public AuctionDTO extendAuction(Long auctionId, int minutes) {
        log.info("Extending auction {} by {} minutes", auctionId, minutes);
        Auction auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            log.warn("Attempted to extend non-active auction {}", auctionId);
            return convertToDTO(auction);
        }

        auction.setEndTime(auction.getEndTime().plusMinutes(minutes));
        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} extended. New end time: {}", auctionId, saved.getEndTime());

        // Broadcast to bidding-service
        try {
            String msg = String.format("⏰ TIME EXTENDED! Auction #%d '%s' extended by %d mins (New end: %02d:%02d)", 
                saved.getId(), saved.getTitle(), minutes, saved.getEndTime().getHour(), saved.getEndTime().getMinute());
            restTemplate.postForObject(biddingServiceUrl + "/api/bids/broadcast", msg, String.class);
        } catch (Exception e) {
            log.error("Failed to broadcast time extension", e);
        }

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
     * Cancel an auction (can be called from any non-closed state).
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

        auction.validateForCreation();

        Auction saved = auctionRepository.save(auction);
        log.info("Auction {} updated successfully", auctionId);

        return convertToDTO(saved);
    }

    private Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.warn("Auction not found: {}", auctionId);
                    return new IllegalArgumentException("Auction not found: " + auctionId);
                });
    }

    private AuctionDTO convertToDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.setId(auction.getId());
        dto.setTitle(auction.getTitle());
        dto.setDescription(auction.getDescription());
        dto.setPrice(auction.getPrice());
        dto.setReservePrice(auction.getReservePrice());
        dto.setImageUrl(auction.getImageUrl());
        dto.setSellerId(auction.getSellerId());
        dto.setStartTime(auction.getStartTime());
        dto.setEndTime(auction.getEndTime());
        dto.setStatus(auction.getStatus());
        dto.setCreatedAt(auction.getCreatedAt());
        dto.setUpdatedAt(auction.getUpdatedAt());
        dto.setScheduledAt(auction.getScheduledAt());
        dto.setActivatedAt(auction.getActivatedAt());
        dto.setEndedAt(auction.getEndedAt());
        dto.setClosedAt(auction.getClosedAt());
        dto.setClosureReason(auction.getClosureReason());
        return dto;
    }
}
