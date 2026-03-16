package com.campus.bidding.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for bid placement requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidDTO {

    private Long id;

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    private Long bidderId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be positive")
    private BigDecimal amount;

    private String status;

    private LocalDateTime placedAt;
}
