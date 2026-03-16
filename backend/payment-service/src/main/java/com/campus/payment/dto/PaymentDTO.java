package com.campus.payment.dto;

import com.campus.payment.model.DisputeStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment requests and transaction responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    private Long id;

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    private Long buyerId;

    private Long sellerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private DisputeStatus status;

    private String paymentReference;

    private String trackingInfo;

    private String disputeReason;

    private LocalDateTime createdAt;
}
