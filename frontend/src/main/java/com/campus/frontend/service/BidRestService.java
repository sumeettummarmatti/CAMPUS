package com.campus.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * REST client for the Bidding Service (port 8083).
 */
public class BidRestService {

    private static final String API_URL = AppConfig.biddingServiceUrl() + "/api/bids";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String authToken;

    public BidRestService(String authToken) {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.authToken = authToken;
    }

    /**
     * Place a bid on an auction.
     * Returns the saved BidDTO as a JsonNode.
     */
    public JsonNode placeBid(Long auctionId, Long buyerId, Double amount) throws Exception {
        String json = String.format(
            "{\"auctionId\":%d,\"buyerId\":%d,\"amount\":%.2f}",
            auctionId, buyerId, amount
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + authToken)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 201) {
            return mapper.readTree(response.body());
        } else {
            JsonNode err = mapper.readTree(response.body());
            String msg = err.has("message") ? err.get("message").asText() : response.body();
            throw new Exception("Bid failed: " + msg);
        }
    }

    /**
     * Fetch all bids for an auction (bid history).
     */
    public JsonNode getBidsForAuction(Long auctionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/auction/" + auctionId))
            .header("Authorization", "Bearer " + authToken)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );
        return mapper.readTree(response.body());
    }
}