package com.campus.auction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing an Auction item.
 * Includes all details for auction CRUD, scheduling, and lifecycle management.
 */
@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Auction title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false)
    private Double price;

    @NotNull(message = "Reserve price is required")
    @DecimalMin(value = "0.0", message = "Reserve price cannot be negative")
    @Column(nullable = false)
    private Double reservePrice;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @NotNull(message = "Seller ID is required")
    @Column(nullable = false)
    private Long sellerId;

    @NotNull(message = "Start time is required")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Audit fields
    private LocalDateTime scheduledAt;
    private LocalDateTime activatedAt;
    private LocalDateTime endedAt;
    private LocalDateTime closedAt;
    private String closureReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AuctionStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Validates auction fields before creating/scheduling.
     * @throws IllegalArgumentException if validation fails
     */
    public void validateForCreation() {
        if (this.startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        }
        if (this.endTime.isBefore(this.startTime) || this.endTime.equals(this.startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (this.price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (this.reservePrice < 0) {
            throw new IllegalArgumentException("Reserve price cannot be negative");
        }
    }
}
