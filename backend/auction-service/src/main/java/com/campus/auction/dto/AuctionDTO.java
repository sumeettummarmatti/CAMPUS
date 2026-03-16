package com.campus.auction.dto;

import com.campus.auction.model.AuctionStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for auction create/update requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionDTO {

    private Long id;

    private Long sellerId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String imageUrl;

    private String category;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be positive")
    private BigDecimal startingPrice;

    private BigDecimal reservePrice;

    private BigDecimal currentHighestBid;

    private AuctionStatus status;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private LocalDateTime createdAt;
}
