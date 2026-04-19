package com.campus.bidding.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class BidWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> auctionSessions =
        new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String auctionId = extractAuctionId(session);
        auctionSessions
            .computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>())
            .add(session);
        System.out.printf("[WS] Client connected — auction %s, total sessions: %d%n",
            auctionId, auctionSessions.get(auctionId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String auctionId = extractAuctionId(session);
        Set<WebSocketSession> sessions = auctionSessions.get(auctionId);
        if (sessions != null) {
            sessions.remove(session);
        }
        System.out.printf("[WS] Client disconnected — auction %s%n", auctionId);
    }

    public void broadcastBidUpdate(Long auctionId, Double newHighestBid) {
        String message = String.format(
            "{\"type\":\"NEW_BID\",\"auctionId\":%d,\"newHighestBid\":%.2f}", auctionId, newHighestBid
        );
        broadcastMessage(auctionId, message);
    }

    public void broadcastMessage(Long auctionId, String rawJsonMessage) {
        String key = String.valueOf(auctionId);
        Set<WebSocketSession> sessions = auctionSessions.getOrDefault(key, Set.of());

        for (WebSocketSession session : sessions) {
            sendSafely(session, rawJsonMessage);
        }
    }

    public void broadcastToAll(String rawJsonMessage) {
        auctionSessions.values().forEach(sessions -> {
            for (WebSocketSession session : sessions) {
                sendSafely(session, rawJsonMessage);
            }
        });
    }

    private void sendSafely(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.printf("[WS] Send failed for session %s: %s%n", session.getId(), e.getMessage());
            }
        }
    }

    // Pulls "42" out of a path like /ws/auction/42
    private String extractAuctionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}