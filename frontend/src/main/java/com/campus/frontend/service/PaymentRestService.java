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

    /** Simulates a buyer paying for a PENDING transaction */
    public void payTransaction(Long transactionId) throws Exception {
        payWithMethod(transactionId, "CAMPUS_WALLET");
    }

    public void payWithMethod(Long transactionId, String method) throws Exception {
        // Step 1: submit to payment factory flow (PENDING -> PAYMENT_PROCESSING)
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/confirm"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req1, HttpResponse.BodyHandlers.ofString());

        // Step 2: move to escrow
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/hold"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req2, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Completes the full escrow lifecycle for a campus auction:
     * IN_ESCROW -> SHIPPED -> DELIVERY_CONFIRMED -> COMPLETED
     * This triggers seller earnings to be credited.
     */
    public void autoCompleteEscrow(Long transactionId) throws Exception {
        // Step 3: mark shipped
        HttpRequest req3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/ship"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req3, HttpResponse.BodyHandlers.ofString());

        // Step 4: confirm delivery
        HttpRequest req4 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/confirm-delivery"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req4, HttpResponse.BodyHandlers.ofString());

        // Step 5: release funds to seller (COMPLETED)
        HttpRequest req5 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/release"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req5, HttpResponse.BodyHandlers.ofString());
    }

    /** Initiates a payment for a won auction */
    public JsonNode initiatePayment(Long auctionId, Long winnerId, Long sellerId, double amount, String method) throws Exception {
        String body = String.format(
            "{\"auctionId\":%d, \"winnerId\":%d, \"sellerId\":%d, \"amount\":%.2f, \"paymentMethod\":\"%s\"}",
            auctionId, winnerId, sellerId, amount, method
        );
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/initiate"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
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

    /** Seller marks item as shipped (IN_ESCROW → SHIPPED) */
    public void markShipped(Long transactionId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/ship"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not mark as shipped: " + resp.body());
        }
    }

    /** Release funds to seller after delivery confirmed (DELIVERY_CONFIRMED → COMPLETED) */
    public void releaseFunds(Long transactionId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + transactionId + "/escrow/release"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not release funds: " + resp.body());
        }
    }
}
