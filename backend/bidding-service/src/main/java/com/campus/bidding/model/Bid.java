package com.campus.bidding.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;

@Entity
@Table(name = "bids")
// if two bids hit the DB at the same time, only one commits.
// The other gets an OptimisticLockException, which we catch
// and resolve by picking the higher amount
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Version
    private Long version;

    public Bid() {
    }

    public Bid(Long id, Long auctionId, Long buyerId, Double amount, BidStatus status, LocalDateTime placedAt, Long version) {
        this.id = id;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.status = status;
        this.placedAt = placedAt;
        this.version = version;
    }

    @PrePersist
    protected void onCreate() {
        this.placedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BidStatus.PENDING;
        }
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

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public static BidBuilder builder() {
        return new BidBuilder();
    }

    public static class BidBuilder {
        private Long id;
        private Long auctionId;
        private Long buyerId;
        private Double amount;
        private BidStatus status;
        private LocalDateTime placedAt;
        private Long version;

        public BidBuilder id(Long id) { this.id = id; return this; }
        public BidBuilder auctionId(Long auctionId) { this.auctionId = auctionId; return this; }
        public BidBuilder buyerId(Long buyerId) { this.buyerId = buyerId; return this; }
        public BidBuilder amount(Double amount) { this.amount = amount; return this; }
        public BidBuilder status(BidStatus status) { this.status = status; return this; }
        public BidBuilder placedAt(LocalDateTime placedAt) { this.placedAt = placedAt; return this; }
        public BidBuilder version(Long version) { this.version = version; return this; }

        public Bid build() {
            return new Bid(id, auctionId, buyerId, amount, status, placedAt, version);
        }
    }
}