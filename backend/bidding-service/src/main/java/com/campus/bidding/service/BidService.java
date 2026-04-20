package com.campus.bidding.service;

import com.campus.bidding.dto.BidDTO;
import com.campus.bidding.model.Bid;
import com.campus.bidding.model.BidStatus;
import com.campus.bidding.repository.BidRepository;
import com.campus.bidding.websocket.BidWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

import java.util.List;

@Service
@Transactional
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    private final BidRepository bidRepository;
    private final BidWebSocketHandler webSocketHandler;
    private final RestTemplate restTemplate;

    @Value("${services.auction-service-url}")
    private String auctionServiceUrl;

    @Value("${services.user-service-url}")
    private String userServiceUrl;

    public BidService(BidRepository bidRepository, BidWebSocketHandler webSocketHandler, RestTemplate restTemplate) {
        this.bidRepository = bidRepository;
        this.webSocketHandler = webSocketHandler;
        this.restTemplate = restTemplate;
    }

    public BidDTO placeBid(BidDTO dto) {
        log.info("Received bid placement request for auction {}, buyer {}, amount {}",
            dto.getAuctionId(), dto.getBuyerId(), dto.getAmount());

        // 0. Ensure buyer is not the seller
        String url = auctionServiceUrl + "/api/auctions/" + dto.getAuctionId();
        try {
            Map<String, Object> auction = restTemplate.getForObject(url, Map.class);
            if (auction != null) {
                if (auction.get("sellerId") != null) {
                    Long sellerId = Long.valueOf(auction.get("sellerId").toString());
                    if (dto.getBuyerId().equals(sellerId)) {
                        throw new IllegalArgumentException("Sellers cannot bid on their own auctions.");
                    }
                }
                if (auction.get("price") != null) {
                    Double startingPrice = Double.valueOf(auction.get("price").toString());
                    if (dto.getAmount() <= startingPrice) {
                        throw new IllegalArgumentException("Bid amount must be greater than starting price of " + startingPrice);
                    }
                }
                if (auction.get("status") != null) {
                    String status = auction.get("status").toString();
                    if (!"ACTIVE".equals(status)) {
                        throw new IllegalArgumentException("Bidding is only allowed on ACTIVE auctions. Current status: " + status);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e; // rethrow the business validation exception
        } catch (Exception e) {
            log.error("Failed to fetch auction details for validation", e);
            throw new IllegalArgumentException("Unable to validate auction: " + e.getMessage());
        }

        // --- AUTO-EXTENSION LOGIC ---
        try {
            String endTimeStr = restTemplate.getForObject(auctionServiceUrl + "/api/auctions/" + dto.getAuctionId(), Map.class).get("endTime").toString();
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(endTimeStr);
            java.time.Duration remaining = java.time.Duration.between(java.time.LocalDateTime.now(), endTime);
            
            if (remaining.getSeconds() > 0 && remaining.getSeconds() <= 30) {
                log.info("Bid placed in last 30s for auction {}. Extending by 2 mins...", dto.getAuctionId());
                restTemplate.postForObject(auctionServiceUrl + "/api/auctions/" + dto.getAuctionId() + "/extend?minutes=2", null, String.class);
            }
        } catch (Exception e) {
            log.warn("Auto-extension check failed for auction {}: {}", dto.getAuctionId(), e.getMessage());
        }
        // ---------------------------

        // 1. Ensure new bid is higher than current highest bid
        List<Bid> bids = bidRepository.findByAuctionIdOrderByAmountDesc(dto.getAuctionId());
        if (!bids.isEmpty()) {
            Bid highest = bids.get(0);
            if (dto.getAmount() <= highest.getAmount()) {
                throw new IllegalArgumentException("Bid amount " + dto.getAmount() +
                    " must be greater than current highest bid " + highest.getAmount());
            }

            // Mark the old highest as OUTBID
            highest.setStatus(BidStatus.OUTBID);
            bidRepository.save(highest);
        }

        // 2. Insert new bid
        Bid bid = Bid.builder()
            .auctionId(dto.getAuctionId())
            .buyerId(dto.getBuyerId())
            .amount(dto.getAmount())
            .status(BidStatus.LEADING)
            .build();

        Bid saved = bidRepository.save(bid);

        BidDTO response = mapToDTO(saved);

        // 3. Broadcast new highest bid to WebSocket listeners
        webSocketHandler.broadcastBidUpdate(dto.getAuctionId(), dto.getAmount());

        return response;
    }

    @Transactional(readOnly = true)
    public List<BidDTO> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
            .stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<BidDTO> getBidsByBuyer(Long buyerId) {
        return bidRepository.findByBuyerIdOrderByPlacedAtDesc(buyerId)
            .stream().map(this::mapToDTO).toList();
    }

    /**
     * Resolves the ending of an auction. The highest LEADING bid becomes WON,
     * and any others become LOST (if they weren't already OUTBID).
     */
    public void resolveAuctionEnd(Long auctionId) {
        log.info("Resolving end of auction {}", auctionId);
        List<Bid> bids = bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);
        if (bids.isEmpty()) {
            log.info("No bids to resolve for auction {}", auctionId);
            return;
        }

        // The first bid is the highest
        Bid winner = bids.get(0);
        winner.setStatus(BidStatus.WON);
        bidRepository.save(winner);
        log.info("Bid {} declared WON for auction {}", winner.getId(), auctionId);

        // All other bids are LOST or OUTBID
        for (int i = 1; i < bids.size(); i++) {
            Bid loser = bids.get(i);
            if (loser.getStatus() != BidStatus.OUTBID) {
                loser.setStatus(BidStatus.LOST);
                bidRepository.save(loser);
            }
        }
        
        // Fetch winner name from user-service
        String winnerName = "User #" + winner.getBuyerId();
        try {
            String userUrl = userServiceUrl + "/api/users/" + winner.getBuyerId();
            Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);
            if (user != null && user.get("email") != null) {
                winnerName = user.get("email").toString();
            }
        } catch (Exception e) {
            log.error("Failed to fetch winner details for broadcast", e);
        }

        // Fetch auction to get sellerId
        Long sellerId = 0L;
        String title = "Auction #" + auctionId;
        try {
            Map<String, Object> auction = restTemplate.getForObject(auctionServiceUrl + "/api/auctions/" + auctionId, Map.class);
            if (auction != null) {
                sellerId = Long.valueOf(auction.get("sellerId").toString());
                title = auction.get("title").toString();
            }
        } catch (Exception ignore) {}

        // Broadcast auction end
        String endMsg = String.format(
            "{\"type\":\"AUCTION_ENDED\",\"auctionId\":%d,\"sellerId\":%d,\"winnerId\":%d,\"title\":\"%s\",\"winnerName\":\"%s\",\"amount\":%.2f}",
            auctionId, sellerId, winner.getBuyerId(), title, winnerName, winner.getAmount());
        webSocketHandler.broadcastMessage(auctionId, endMsg);
        webSocketHandler.broadcastToAll(endMsg);
    }

    private BidDTO mapToDTO(Bid bid) {
        return BidDTO.builder()
            .id(bid.getId())
            .auctionId(bid.getAuctionId())
            .buyerId(bid.getBuyerId())
            .amount(bid.getAmount())
            .status(bid.getStatus())
            .placedAt(bid.getPlacedAt())
            .build();
    }
}
