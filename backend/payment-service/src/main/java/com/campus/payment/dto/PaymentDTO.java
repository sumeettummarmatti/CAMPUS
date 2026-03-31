package com.campus.payment.dto;

import com.campus.payment.model.DisputeStatus;
import com.campus.payment.model.PaymentMethod;
import com.campus.payment.model.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a payment transaction response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
