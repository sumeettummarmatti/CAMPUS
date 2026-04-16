package com.campus.bidding.dto;

import com.campus.bidding.model.BidStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}