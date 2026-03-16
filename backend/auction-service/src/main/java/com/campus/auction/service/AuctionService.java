package com.campus.auction.service;

import com.campus.auction.dto.AuctionDTO;
import com.campus.auction.model.Auction;
import com.campus.auction.pattern.AuctionBuilder;
import com.campus.auction.model.AuctionStatus;
import com.campus.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business-logic layer for auction management (MVC: Service).
 */
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;

    /**
     * Create a new auction in DRAFT → SCHEDULED state.
     */
    public AuctionDTO createAuction(AuctionDTO dto, Long sellerId) {
        Auction auction = new AuctionBuilder()
                .withSellerId(sellerId)
                .withTitle(dto.getTitle())
                .withDescription(dto.getDescription())
                .withImageUrl(dto.getImageUrl())
                .withCategory(dto.getCategory())
                .withStartingPrice(dto.getStartingPrice())
                .withReservePrice(dto.getReservePrice())
                .withSchedule(dto.getStartTime(), dto.getEndTime())
                .build();

        Auction saved = auctionRepository.save(auction);
        return toDTO(saved);
    }

    /**
     * Retrieve a single auction by ID.
     */
    public AuctionDTO getAuction(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + id));
        return toDTO(auction);
    }

    /**
     * List all ACTIVE auctions.
     */
    public List<AuctionDTO> listActive() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * List all auctions (any status).
     */
    public List<AuctionDTO> listAll() {
        return auctionRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * List auctions by a specific seller.
     */
    public List<AuctionDTO> listBySeller(Long sellerId) {
        return auctionRepository.findBySellerId(sellerId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Manually close an auction (seller or admin action).
     */
    @Transactional
    public AuctionDTO closeAuction(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE auctions can be closed");
        }

        auction.setStatus(AuctionStatus.ENDED);
        Auction saved = auctionRepository.save(auction);
        return toDTO(saved);
    }

    /**
     * Cancel an auction (seller cancels before start, or admin intervention).
     */
    @Transactional
    public AuctionDTO cancelAuction(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.SCHEDULED
                && auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction cannot be cancelled in its current state");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);
        return toDTO(saved);
    }

    /**
     * Update the highest bid (called by bidding-service via REST).
     */
    @Transactional
    public void updateHighestBid(Long auctionId, BigDecimal amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + auctionId));

        auction.setCurrentHighestBid(amount);
        auctionRepository.save(auction);
    }

    /**
     * Extend auction end time (anti-snipe rule).
     */
    @Transactional
    public void extendEndTime(Long auctionId, int minutes) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + auctionId));

        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setEndTime(auction.getEndTime().plusMinutes(minutes));
            auctionRepository.save(auction);
        }
    }

    // ── Mapping ───────────────────────────────────────────

    private AuctionDTO toDTO(Auction a) {
        return AuctionDTO.builder()
                .id(a.getId())
                .sellerId(a.getSellerId())
                .title(a.getTitle())
                .description(a.getDescription())
                .imageUrl(a.getImageUrl())
                .category(a.getCategory())
                .startingPrice(a.getStartingPrice())
                .reservePrice(a.getReservePrice())
                .currentHighestBid(a.getCurrentHighestBid())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
