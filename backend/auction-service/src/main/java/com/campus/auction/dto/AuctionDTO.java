package com.campus.auction.dto;

import com.campus.auction.model.AuctionStatus;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Auction API requests/responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionDTO {

    private Long id;
    private String title;
    private String description;
    private Double price;
    private Double reservePrice;
    private String imageUrl;
    private Long sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime activatedAt;
    private LocalDateTime endedAt;
    private LocalDateTime closedAt;
    private String closureReason;
}
