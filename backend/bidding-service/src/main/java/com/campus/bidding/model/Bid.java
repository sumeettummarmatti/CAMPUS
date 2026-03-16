package com.campus.bidding.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a single bid on an auction.
 * Uses @Version for optimistic locking to handle concurrent bids.
 */
@Entity
@Table(name = "bids")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long bidderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;    // ACCEPTED, LEADING, OUTBID, WON, LOST, REJECTED

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.placedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACCEPTED";
        }
    }
}
