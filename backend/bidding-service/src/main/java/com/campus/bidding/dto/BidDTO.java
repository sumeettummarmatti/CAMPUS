package com.campus.bidding.dto;

import com.campus.bidding.model.BidStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class BidDTO {

    private Long id;

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    @NotNull(message = "Buyer ID is required")
    private Long buyerId;

    @NotNull(message = "Bid amount is required")
    @Positive(message = "Bid amount must be positive")
    private Double amount;

    private BidStatus status;
    private LocalDateTime placedAt;

    public BidDTO() {}

    public BidDTO(Long id, Long auctionId, Long buyerId, Double amount, BidStatus status, LocalDateTime placedAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.status = status;
        this.placedAt = placedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public BidStatus getStatus() { return status; }
    public void setStatus(BidStatus status) { this.status = status; }

    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }

    public static BidDTOBuilder builder() {
        return new BidDTOBuilder();
    }

    public static class BidDTOBuilder {
        private Long id;
        private Long auctionId;
        private Long buyerId;
        private Double amount;
        private BidStatus status;
        private LocalDateTime placedAt;

        public BidDTOBuilder id(Long id) { this.id = id; return this; }
        public BidDTOBuilder auctionId(Long auctionId) { this.auctionId = auctionId; return this; }
        public BidDTOBuilder buyerId(Long buyerId) { this.buyerId = buyerId; return this; }
        public BidDTOBuilder amount(Double amount) { this.amount = amount; return this; }
        public BidDTOBuilder status(BidStatus status) { this.status = status; return this; }
        public BidDTOBuilder placedAt(LocalDateTime placedAt) { this.placedAt = placedAt; return this; }

        public BidDTO build() {
            return new BidDTO(id, auctionId, buyerId, amount, status, placedAt);
        }
    }
}