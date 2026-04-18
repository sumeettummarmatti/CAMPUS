package com.campus.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.frontend.model.User;
import com.campus.frontend.config.AppConfig;

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
    public User login(String email, String password) throws Exception {
        String jsonPayload = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> loginResp = httpClient.send(loginReq, HttpResponse.BodyHandlers.ofString());

        if (loginResp.statusCode() != 200) {
            throw new Exception("Login failed: " + loginResp.body());
        }

        JsonNode root = mapper.readTree(loginResp.body());
        this.authToken = root.path("token").asText();

        // Decode role from JWT claims (the payload is base64, second segment)
        String[] parts = authToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = mapper.readTree(payload);

        User user = new User();
        user.setEmail(email);
        user.setRole(claims.path("role").asText("BUYER"));
        // Fetch full profile from user service
        fetchAndFillProfile(user);
        return user;
    }

    private void fetchAndFillProfile(User user) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.userServiceUrl() + "/api/users/me"))
                    .header("Authorization", "Bearer" + authToken)
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode profile = mapper.readTree(resp.body());
                user.setId(profile.path("id").asLong());
                user.setFullName(profile.path("fullName").asText());
            }
        } catch (Exception e) {
            System.err.println("Could not fetch profile: " + e.getMessage());
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
