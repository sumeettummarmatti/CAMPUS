package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.service.AuctionRestService;
import com.campus.frontend.service.BidRestService;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for the Auction Detail popup dialog.
 * Shows full description, prices, timing and live highest bid.
 */
public class AuctionDetailController {

    @FXML private Label titleLabel;
    @FXML private Label idLabel;
    @FXML private Label statusLabel;
    @FXML private Label descLabel;
    @FXML private Label priceLabel;
    @FXML private Label reserveLabel;
    @FXML private Label highestBidLabel;
    @FXML private Label sellerLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label loadingLabel;

    private AuctionRestService auctionRestService;
    private BidRestService bidRestService;

    @FXML
    public void initialize() {
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        auctionRestService = new AuctionRestService(token);
        bidRestService = new BidRestService(token);
    }

    /**
     * Populate the dialog using a pre-fetched JsonNode auction object.
     * Also fires off an async request to load the live highest bid.
     */
    public void loadAuction(JsonNode auction) {
        long auctionId = auction.path("id").asLong();
        String title   = auction.path("title").asText("—");
        String desc    = auction.path("description").asText("No description provided.");
        double price   = auction.path("price").asDouble();
        double reserve = auction.path("reservePrice").asDouble();
        String status  = auction.path("status").asText("—");
        String startT  = auction.path("startTime").asText("—");
        String endT    = auction.path("endTime").asText("—");
        long sellerId  = auction.path("sellerId").asLong(-1L);

        titleLabel.setText(title);
        idLabel.setText("#" + auctionId);
        descLabel.setText(desc);
        priceLabel.setText(String.format("₹%.2f", price));
        reserveLabel.setText(reserve > 0 ? String.format("₹%.2f", reserve) : "None");
        startTimeLabel.setText(formatTime(startT));
        endTimeLabel.setText(formatTime(endT));
        sellerLabel.setText(sellerId > 0 ? String.valueOf(sellerId) : "—");

        // Colour-code status
        statusLabel.setText(status);
        switch (status.toUpperCase()) {
            case "ACTIVE"     -> statusLabel.setStyle(statusLabel.getStyle() + "; -fx-text-fill: #34d399;");
            case "SCHEDULED"  -> statusLabel.setStyle(statusLabel.getStyle() + "; -fx-text-fill: #60a5fa;");
            case "ENDED", "CLOSED_NO_SALE", "CLOSED_SOLD" ->
                statusLabel.setStyle(statusLabel.getStyle() + "; -fx-text-fill: #f87171;");
            default           -> statusLabel.setStyle(statusLabel.getStyle() + "; -fx-text-fill: #94a3b8;");
        }

        // Async: fetch live highest bid
        loadingLabel.setText("Fetching live bid...");
        new Thread(() -> {
            try {
                JsonNode bids = bidRestService.getBidsForAuction(auctionId);
                String bidText;
                if (bids.isArray() && bids.size() > 0) {
                    double highest = bids.get(0).path("amount").asDouble();
                    bidText = String.format("₹%.2f", highest);
                } else {
                    bidText = "No bids yet";
                }
                Platform.runLater(() -> {
                    highestBidLabel.setText(bidText);
                    loadingLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    highestBidLabel.setText("Could not load bid");
                    loadingLabel.setText("");
                });
            }
        }).start();
    }

    /**
     * Alternative entry: load by auction ID (for scheduled auctions which may not be in the list node).
     */
    public void loadAuctionById(Long auctionId) {
        loadingLabel.setText("Loading auction details...");
        new Thread(() -> {
            try {
                JsonNode auction = auctionRestService.getAuctionById(auctionId);
                Platform.runLater(() -> loadAuction(auction));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    titleLabel.setText("Error loading auction");
                    loadingLabel.setText(e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void onClose(ActionEvent e) {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }

    /** Trims "T" separator for nicer display. */
    private String formatTime(String iso) {
        if (iso == null || iso.equals("—")) return "—";
        return iso.replace("T", "  ");
    }
}
