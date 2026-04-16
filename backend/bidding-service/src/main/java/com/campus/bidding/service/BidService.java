package com.campus.bidding.service;

import com.campus.bidding.dto.BidDTO;
import com.campus.bidding.model.Bid;
import com.campus.bidding.model.BidStatus;
import com.campus.bidding.repository.BidRepository;
import com.campus.bidding.websocket.BidWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BidService {

    private static final double MIN_INCREMENT = 1.0;

    private final BidRepository bidRepository;
    private final NotificationService notificationService;
    private final BidWebSocketHandler webSocketHandler;

    @Transactional
    public BidDTO placeBid(BidDTO dto) {

        // Find whoever is currently leading this auction
        Optional<Bid> currentLeader = bidRepository
            .findTopByAuctionIdAndStatusOrderByAmountDesc(
                dto.getAuctionId(), BidStatus.LEADING
            );

        double currentHighest = currentLeader.map(Bid::getAmount).orElse(0.0);

        // Reject if the new amount doesn't beat the minimum increment
        if (dto.getAmount() < currentHighest + MIN_INCREMENT) {
            throw new IllegalArgumentException(String.format(
                "Bid too low. Minimum required: %.2f", currentHighest + MIN_INCREMENT
            ));
        }

        // Reject if this buyer is already leading
        if (bidRepository.existsByAuctionIdAndBuyerIdAndStatus(
                dto.getAuctionId(), dto.getBuyerId(), BidStatus.LEADING)) {
            throw new IllegalStateException("You are already the highest bidder.");
        }

        Bid newBid = Bid.builder()
            .auctionId(dto.getAuctionId())
            .buyerId(dto.getBuyerId())
            .amount(dto.getAmount())
            .status(BidStatus.ACCEPTED)
            .build();

        try {
            // Demote the previous leader
            currentLeader.ifPresent(prev -> {
                prev.setStatus(BidStatus.OUTBID);
                bidRepository.save(prev);
                notificationService.sendOutbidNotification(
                    prev.getBuyerId(), prev.getAuctionId(), dto.getAmount()
                );
            });

            // Save and promote the new bid
            newBid.setStatus(BidStatus.LEADING);
            Bid saved = bidRepository.save(newBid);

            notificationService.sendBidConfirmation(
                saved.getBuyerId(), saved.getAuctionId(), saved.getAmount()
            );
            // Broadcast live update to all WebSocket clients watching this auction
            webSocketHandler.broadcastBidUpdate(saved.getAuctionId(), saved.getAmount());

            return toDTO(saved);

        } catch (ObjectOptimisticLockingFailureException e) {
            // Two bids arrived at exactly the same time.
            // The @Version field caught the conflict — reject the loser cleanly.
            throw new IllegalStateException(
                "Another bid was placed at the same time. Please try again."
            );
        }
    }

    @Transactional
    public void resolveAuctionEnd(Long auctionId) {
        List<Bid> allBids = bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);
        boolean winnerFound = false;

        for (Bid bid : allBids) {
            if (!winnerFound && bid.getStatus() == BidStatus.LEADING) {
                bid.setStatus(BidStatus.WON);
                bidRepository.save(bid);
                notificationService.sendWinNotification(
                    bid.getBuyerId(), auctionId, bid.getAmount()
                );
                winnerFound = true;
            } else if (bid.getStatus() == BidStatus.OUTBID
                    || bid.getStatus() == BidStatus.LEADING) {
                bid.setStatus(BidStatus.LOST);
                bidRepository.save(bid);
                notificationService.sendLossNotification(bid.getBuyerId(), auctionId);
            }
        }
    }

    public List<BidDTO> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
            .stream().map(this::toDTO).toList();
    }

    public List<BidDTO> getBidsByBuyer(Long buyerId) {
        return bidRepository.findByBuyerIdOrderByPlacedAtDesc(buyerId)
            .stream().map(this::toDTO).toList();
    }

    private BidDTO toDTO(Bid bid) {
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