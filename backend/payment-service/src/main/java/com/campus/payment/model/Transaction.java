package com.campus.payment.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a payment transaction between auction winner and seller.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long winnerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private DisputeStatus disputeStatus;

    @Column(length = 1000)
    private String disputeReason;

    /** Stores the TransactionStatus active immediately before a dispute was opened,
     *  so that closeDispute can restore the transaction to the correct state. */
    @Enumerated(EnumType.STRING)
    private TransactionStatus preDisputeStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime releasedAt;

    public Transaction() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public DisputeStatus getDisputeStatus() { return disputeStatus; }
    public void setDisputeStatus(DisputeStatus disputeStatus) { this.disputeStatus = disputeStatus; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public TransactionStatus getPreDisputeStatus() { return preDisputeStatus; }
    public void setPreDisputeStatus(TransactionStatus preDisputeStatus) { this.preDisputeStatus = preDisputeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    // ── Builder ─────────────────────────────────────────────────────

    public static TransactionBuilder builder() { return new TransactionBuilder(); }

    public static class TransactionBuilder {
        private final Transaction t = new Transaction();

        public TransactionBuilder auctionId(Long v) { t.auctionId = v; return this; }
        public TransactionBuilder winnerId(Long v) { t.winnerId = v; return this; }
        public TransactionBuilder sellerId(Long v) { t.sellerId = v; return this; }
        public TransactionBuilder amount(BigDecimal v) { t.amount = v; return this; }
        public TransactionBuilder status(TransactionStatus v) { t.status = v; return this; }
        public TransactionBuilder paymentMethod(PaymentMethod v) { t.paymentMethod = v; return this; }
        public TransactionBuilder disputeStatus(DisputeStatus v) { t.disputeStatus = v; return this; }
        public TransactionBuilder disputeReason(String v) { t.disputeReason = v; return this; }
        public TransactionBuilder preDisputeStatus(TransactionStatus v) { t.preDisputeStatus = v; return this; }

        public Transaction build() { return t; }
    }
}
