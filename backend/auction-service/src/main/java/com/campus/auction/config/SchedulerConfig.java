package com.campus.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduling automatic auction lifecycle transitions.
 * Enables @Scheduled tasks across the application.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Configuration class for scheduling
    // Scheduled tasks are defined in AuctionSchedulerService
}
