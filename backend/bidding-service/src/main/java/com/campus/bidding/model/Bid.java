package com.campus.bidding.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;

@Entity
@Table(name = "bids")
// if two bids hit the DB at the same time, only one commits.
// The other gets an OptimisticLockException, which we catch
// and resolve by picking the higher amount
@OptimisticLocking(type = OptimisticLockType.VERSION)
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
    private Long buyerId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.placedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BidStatus.PENDING;
        }
    }
}