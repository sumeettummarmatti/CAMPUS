package com.campus.auction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Auction Service microservice.
 * 
 * Provides:
 * - Auction CRUD operations
 * - Auction lifecycle management (DRAFT → SCHEDULED → ACTIVE → ENDED → CLOSED_*)
 * - Scheduled tasks for automatic state transitions
 * - REST API for auction operations
 */
@SpringBootApplication
@EnableScheduling
public class AuctionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionServiceApplication.class, args);
    }
}
