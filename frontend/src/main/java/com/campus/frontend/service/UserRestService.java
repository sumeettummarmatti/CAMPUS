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
                    .header("Authorization", "Bearer " + authToken)
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode profile = mapper.readTree(resp.body());
                user.setId(profile.path("id").asLong());
                user.setFullName(profile.path("fullName").asText());
                user.setWalletBalance(profile.path("walletBalance").asDouble(0.0));
                user.setTotalSpent(profile.path("totalSpent").asDouble(0.0));
                user.setTotalEarned(profile.path("totalEarned").asDouble(0.0));
                user.setTotalDeposited(profile.path("totalDeposited").asDouble(0.0));
                if (profile.path("enabledPaymentModes").isArray()) {
                    java.util.List<String> modes = new java.util.ArrayList<>();
                    for (JsonNode n : profile.path("enabledPaymentModes")) {
                        modes.add(n.asText());
                    }
                    user.setEnabledPaymentModes(modes);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch profile: " + e.getMessage());
        }
    }

    public User fetchCurrentUserProfile() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.userServiceUrl() + "/api/users/me"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not fetch profile: " + resp.body());
        }

        JsonNode profile = mapper.readTree(resp.body());
        User user = new User();
        user.setId(profile.path("id").asLong());
        user.setEmail(profile.path("email").asText());
        user.setRole(profile.path("role").asText("BUYER"));
        user.setVerified(profile.path("verified").asBoolean(false));
        user.setFullName(profile.path("fullName").asText());
        user.setWalletBalance(profile.path("walletBalance").asDouble(0.0));
        user.setTotalSpent(profile.path("totalSpent").asDouble(0.0));
        user.setTotalEarned(profile.path("totalEarned").asDouble(0.0));
        user.setTotalDeposited(profile.path("totalDeposited").asDouble(0.0));
        if (profile.path("enabledPaymentModes").isArray()) {
            java.util.List<String> modes = new java.util.ArrayList<>();
            for (JsonNode n : profile.path("enabledPaymentModes")) {
                modes.add(n.asText());
            }
            user.setEnabledPaymentModes(modes);
        }
        return user;
    }

    public java.util.List<String> getWalletModes(Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.userServiceUrl() + "/api/users/" + userId + "/wallet/modes"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not fetch wallet modes: " + resp.body());
        }
        java.util.List<String> modes = new java.util.ArrayList<>();
        JsonNode arr = mapper.readTree(resp.body());
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                modes.add(n.asText());
            }
        }
        return modes;
    }

    public java.util.List<String> updateWalletModes(Long userId, java.util.List<String> modes) throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("modes", modes));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.userServiceUrl() + "/api/users/" + userId + "/wallet/modes"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not update wallet modes: " + resp.body());
        }
        java.util.List<String> updated = new java.util.ArrayList<>();
        JsonNode arr = mapper.readTree(resp.body());
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                updated.add(n.asText());
            }
        }
        return updated;
    }

    public void topUpWallet(Long userId, double amount) throws Exception {
        String body = String.format("{\"amount\":%.2f}", amount);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.userServiceUrl() + "/api/users/" + userId + "/wallet/topup"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Could not top up wallet: " + resp.body());
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

    /**
     * Returns true if the user with the given ID is verified.
     * Returns false if the user doesn't exist or the call fails.
     */
    public boolean isUserVerified(Long userId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(
                        AppConfig.userServiceUrl() + "/api/users/" + userId))
                    .header("Authorization", "Bearer " + authToken)
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode profile = mapper.readTree(resp.body());
                return profile.path("verified").asBoolean(false);
            }
        } catch (Exception e) {
            // Silently fail — just show as unverified
        }
        return false;
    }
}
