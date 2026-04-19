package com.campus.frontend.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * In-app notification derived from bid status changes (OUTBID / WON / LOST).
 */
public class BidNotification {

    public enum Type { OUTBID, WON, LOST, LEADING }

    private final Long auctionId;
    private final String message;
    private final Type type;
    private final LocalDateTime timestamp;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd-MMM HH:mm");

    public BidNotification(Long auctionId, String message, Type type, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.message   = message;
        this.type      = type;
        this.timestamp = timestamp;
    }

    public Long          getAuctionId() { return auctionId; }
    public String        getMessage()   { return message; }
    public Type          getType()      { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        String icon = switch (type) {
            case OUTBID  -> "🔔";
            case WON     -> "🏆";
            case LOST    -> "❌";
            case LEADING -> "🟢";
        };
        String ts = timestamp != null ? timestamp.format(FMT) : "–";
        return String.format("%s [%s] %s", icon, ts, message);
    }
}
