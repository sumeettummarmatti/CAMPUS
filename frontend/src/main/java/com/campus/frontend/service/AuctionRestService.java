package com.campus.frontend.service;

import com.campus.frontend.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuctionRestService {
    
    private final String BASE = AppConfig.auctionServiceUrl() + "/api/auctions";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String token;

    public AuctionRestService(String token) { this.token = token; }

    /** Returns the list of active auctions for buyer home page */
    public JsonNode getActiveAuctions() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/browse/active?size=50"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode page = mapper.readTree(resp.body());
        return page.path("content"); // Spring Page wraps items in "content"
    }

    /** Returns the list of scheduled auctions for global announcements */
    public JsonNode getScheduledAuctions() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/status/SCHEDULED?size=50"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode page = mapper.readTree(resp.body());
        return page.path("content");
    }

    /** Returns auctions created by a specific seller */
    public JsonNode getMyAuctions(Long sellerId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/seller/" + sellerId + "?size=50"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode page = mapper.readTree(resp.body());
        return page.path("content");
    }

    public JsonNode createAuction(String title, String desc, double price,
                                    double reserve, String startTime,
                                    String endTime, Long sellerId) throws Exception {
        String body = String.format(java.util.Locale.US,
            "{\"title\":\"%s\",\"description\":\"%s\",\"price\":%.2f," +
            "\"reservePrice\":%.2f,\"startTime\":\"%s\",\"endTime\":\"%s\",\"sellerId\":%d}",
            title, desc, price, reserve, startTime, endTime, sellerId);    
            
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            throw new Exception("Create failed: " + resp.body());
        }
        return mapper.readTree(resp.body());
    }

    /** Schedules an auction (draft to scheduled) */
    public void scheduleAuction(Long auctionId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + auctionId + "/schedule"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Schedule failed: " + resp.body());
        }
    }

    /** Terminates an auction early (manual end by seller) */
    public void terminateAuction(Long auctionId, Long sellerId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + auctionId + "/terminate-early?sellerId=" + sellerId))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("Termination failed: " + resp.body());
        }
    }

    /**
     * Extend an auction by X minutes.
     */
    public void extendAuction(Long auctionId, int minutes) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + auctionId + "/extend?minutes=" + minutes))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Extension failed: " + response.body());
        }
    }
}
