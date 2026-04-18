package com.campus.frontend.service;

import com.campus.frontend.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PaymentRestService {

    private final String BASE = AppConfig.paymentServiceUrl() + "/api/payments";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String token;

    public PaymentRestService(String token) { this.token = token; }

    /** All transactions where this user is the buyer (winner) */
    public JsonNode getSpending(Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/winner/" + userId))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }

    /** All transactions where this user is the seller */
    public JsonNode getEarnings(Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/seller/" + userId))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }

    /** Confirm delivery of a shipped item */
    public void confirmDelivery(Long transactionId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/confirm-delivery"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /** Open a dispute */
    public void openDispute(Long transactionId, String reason) throws Exception {
        String body = "{\"reason\":\"" + reason + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.paymentServiceUrl() + "/api/disputes/" + transactionId + "/open"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}