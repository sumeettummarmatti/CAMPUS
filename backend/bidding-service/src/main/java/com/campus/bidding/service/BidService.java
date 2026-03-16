package com.campus.bidding.service;

import com.campus.bidding.dto.BidDTO;
import com.campus.bidding.model.Bid;
import com.campus.bidding.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business-logic layer for bid management (MVC: Service).
 * Handles validation, concurrency (optimistic locking), and
 * broadcasts updates via WebSocket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    /**
     * Place a new bid. Validates amount against current highest,
     * persists the bid, marks previous leader as OUTBID,
     * then broadcasts the update via WebSocket.
     */
    @Transactional
    public BidDTO placeBid(BidDTO dto, Long bidderId) {
        // 1. Check if bidder is already the highest
        Optional<Bid> currentHighest = bidRepository
                .findTopByAuctionIdOrderByAmountDesc(dto.getAuctionId());

        if (currentHighest.isPresent()) {
            Bid highest = currentHighest.get();

            // Reject if bidder already leads
            if (highest.getBidderId().equals(bidderId)) {
                throw new IllegalStateException("You are already the highest bidder");
            }

            // Reject if amount not higher than current highest
            if (dto.getAmount().compareTo(highest.getAmount()) <= 0) {
                throw new IllegalArgumentException(
                        "Bid must be higher than current highest: " + highest.getAmount());
            }

            // Mark previous leader as OUTBID
            highest.setStatus("OUTBID");
            bidRepository.save(highest);
        }

        // 2. Create and persist the new bid
        Bid bid = Bid.builder()
                .auctionId(dto.getAuctionId())
                .bidderId(bidderId)
                .amount(dto.getAmount())
                .status("LEADING")
                .build();

        Bid saved = bidRepository.save(bid);
        log.info("Bid {} placed on auction {} by user {} — amount: {}",
                 saved.getId(), saved.getAuctionId(), bidderId, saved.getAmount());

        // 3. Broadcast via WebSocket
        BidDTO response = toDTO(saved);
        messagingTemplate.convertAndSend(
                "/topic/auction/" + dto.getAuctionId(), response);

        // 4. Notify previous bidder
        currentHighest.ifPresent(prev ->
                notificationService.notifyOutbid(prev.getBidderId(), dto.getAuctionId()));

        return response;
    }

    /**
     * Get all bids for an auction, ordered by amount descending.
     */
    public List<BidDTO> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get the current highest bid for an auction.
     */
    public BidDTO getHighestBid(Long auctionId) {
        Bid highest = bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No bids found for auction: " + auctionId));
        return toDTO(highest);
    }

    // ── Mapping ────────────────────────────────────────

    private BidDTO toDTO(Bid bid) {
        return BidDTO.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .status(bid.getStatus())
                .placedAt(bid.getPlacedAt())
                .build();
    }
}
