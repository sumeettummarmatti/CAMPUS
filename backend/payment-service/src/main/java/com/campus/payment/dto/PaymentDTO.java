package com.campus.payment.dto;

import com.campus.payment.model.DisputeStatus;
import com.campus.payment.model.PaymentMethod;
import com.campus.payment.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a payment transaction response.
 */
public class PaymentDTO {

    private Long id;
    private Long auctionId;
    private Long winnerId;
    private Long sellerId;
    private BigDecimal amount;
    private TransactionStatus status;
    private PaymentMethod paymentMethod;
    private DisputeStatus disputeStatus;
    private String disputeReason;
    private TransactionStatus preDisputeStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime releasedAt;

    public PaymentDTO() {}

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

    public static PaymentDTOBuilder builder() { return new PaymentDTOBuilder(); }

    public static class PaymentDTOBuilder {
        private final PaymentDTO d = new PaymentDTO();

        public PaymentDTOBuilder id(Long v) { d.id = v; return this; }
        public PaymentDTOBuilder auctionId(Long v) { d.auctionId = v; return this; }
        public PaymentDTOBuilder winnerId(Long v) { d.winnerId = v; return this; }
        public PaymentDTOBuilder sellerId(Long v) { d.sellerId = v; return this; }
        public PaymentDTOBuilder amount(BigDecimal v) { d.amount = v; return this; }
        public PaymentDTOBuilder status(TransactionStatus v) { d.status = v; return this; }
        public PaymentDTOBuilder paymentMethod(PaymentMethod v) { d.paymentMethod = v; return this; }
        public PaymentDTOBuilder disputeStatus(DisputeStatus v) { d.disputeStatus = v; return this; }
        public PaymentDTOBuilder disputeReason(String v) { d.disputeReason = v; return this; }
        public PaymentDTOBuilder preDisputeStatus(TransactionStatus v) { d.preDisputeStatus = v; return this; }
        public PaymentDTOBuilder createdAt(LocalDateTime v) { d.createdAt = v; return this; }
        public PaymentDTOBuilder updatedAt(LocalDateTime v) { d.updatedAt = v; return this; }
        public PaymentDTOBuilder releasedAt(LocalDateTime v) { d.releasedAt = v; return this; }

        public PaymentDTO build() { return d; }
    }
}
