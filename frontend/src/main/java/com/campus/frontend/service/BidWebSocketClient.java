package com.campus.frontend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Java 11+ built-in WebSocket client.
 * Connects to ws://localhost:8083/ws/auction/{auctionId}
 * and calls the onMessage callback every time a bid update arrives.
 */
public class BidWebSocketClient {

    private WebSocket webSocket;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * @param auctionId  the auction to subscribe to
     * @param onMessage  callback — receives the raw JSON string on each update
     */
    public void connect(Long auctionId, Consumer<String> onMessage) {
        URI uri = URI.create("ws://localhost:8083/ws/auction/" + auctionId);

        httpClient.newWebSocketBuilder()
            .buildAsync(uri, new WebSocket.Listener() {

                @Override
                public CompletionStage<?> onText(WebSocket ws,
                                                  CharSequence data,
                                                  boolean last) {
                    onMessage.accept(data.toString());
                    return WebSocket.Listener.super.onText(ws, data, last);
                }

                @Override
                public void onError(WebSocket ws, Throwable error) {
                    System.err.println("[WS Client] Error: " + error.getMessage());
                }
            })
            .thenAccept(ws -> {
                this.webSocket = ws;
                System.out.println("[WS Client] Connected to auction " + auctionId);
            });
    }

    public void disconnect() {
        if (webSocket != null && !webSocket.isInputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }
}