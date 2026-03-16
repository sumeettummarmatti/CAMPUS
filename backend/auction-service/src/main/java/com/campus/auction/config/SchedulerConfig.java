package com.campus.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled annotation support.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // No additional configuration needed — @EnableScheduling activates
    // the scheduler that AuctionSchedulerService depends on.
}
