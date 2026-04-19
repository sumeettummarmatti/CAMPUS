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
            if (auction != null && auction.get("sellerId") != null) {
                Long sellerId = Long.valueOf(auction.get("sellerId").toString());
                if (dto.getBuyerId().equals(sellerId)) {
                    throw new IllegalArgumentException("Sellers cannot bid on their own auctions");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e; // rethrow the business validation exception
        } catch (Exception e) {
            log.error("Failed to fetch auction details for validation", e);
            throw new IllegalArgumentException("Unable to validate auction: " + e.getMessage());
        }

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
        
        // Broadcast auction end (the previous broadcastBidUpdate only takes an amount, so we'd need to change it or leave it)
        // Ignoring full JSON broadcast since the WebSocketHandler only accepts (Long, Double) at the moment.
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