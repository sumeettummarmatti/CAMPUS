package com.campus.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Service to communicate with the User Service REST API (port 8081).
 */
public class UserRestService {

    private static final String API_URL = AppConfig.userServiceUrl() + "/api/auth";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    
    private String authToken;

    public String getAuthToken() {
        return authToken;
    }

    public UserRestService() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    /**
     * Login and attempt to get JWT token.
     * Throws exception on failure.
     */
    public boolean login(String email, String password) throws Exception {
        String jsonPayload = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode rootNode = mapper.readTree(response.body());
            this.authToken = rootNode.path("token").asText();
            return true;
        } else {
            throw new Exception("Login failed: " + response.body());
        }
    }

    /**
     * Register a new user.
     */
    public void register(String fullName, String email, String password, String role) throws Exception {
        String jsonPayload = String.format(
            "{\"fullName\":\"%s\", \"email\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}",
            fullName, email, password, role
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new Exception("Registration failed: " + response.body());
        }
    }
}
