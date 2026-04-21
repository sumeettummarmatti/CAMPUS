package com.campus.frontend.service;

import com.campus.frontend.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminRestService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String token;
    private final String USER_BASE;
    private final String PAYMENT_BASE;

    public AdminRestService(String token) {
        this.token = token;
        this.USER_BASE = AppConfig.userServiceUrl() + "/api/users";
        this.PAYMENT_BASE = AppConfig.paymentServiceUrl() + "/api";
    }

    public JsonNode getAllUsers() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USER_BASE))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }

    public void approveSeller(Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USER_BASE + "/" + userId + "/verify/approve"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public void rejectSeller(Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USER_BASE + "/" + userId + "/verify/reject"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public void reviewDispute(Long txId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PAYMENT_BASE + "/disputes/" + txId + "/review"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public void resolveDisputeBuyer(Long txId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PAYMENT_BASE + "/disputes/" + txId + "/resolve/buyer"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public void resolveDisputeSeller(Long txId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PAYMENT_BASE + "/disputes/" + txId + "/resolve/seller"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}