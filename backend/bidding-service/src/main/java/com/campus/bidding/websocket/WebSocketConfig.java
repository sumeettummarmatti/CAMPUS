package com.campus.bidding.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BidWebSocketHandler bidWebSocketHandler;

    public WebSocketConfig(BidWebSocketHandler bidWebSocketHandler) {
        this.bidWebSocketHandler = bidWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Clients connect to: ws://localhost:8083/ws/auction/{auctionId}
        registry.addHandler(bidWebSocketHandler, "/ws/auction/{auctionId}")
                .setAllowedOrigins("*"); // tighten this in prod
    }
}