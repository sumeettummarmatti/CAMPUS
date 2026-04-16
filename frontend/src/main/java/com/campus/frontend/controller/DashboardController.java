package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.service.BidRestService;
import com.campus.frontend.service.BidWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Dashboard — lets a logged-in buyer place bids and see live updates.
 */
public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TextField auctionIdField;
    @FXML private TextField buyerIdField;
    @FXML private TextField bidAmountField;
    @FXML private Label bidStatusLabel;
    @FXML private Label liveHighestBidLabel;
    @FXML private Button connectWsButton;

    private BidRestService bidRestService;
    private BidWebSocketClient webSocketClient;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome to CAMPUS! You are securely logged in.");
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        bidRestService = new BidRestService(token);
        webSocketClient = new BidWebSocketClient();
    }

    /** Subscribe to live WebSocket updates for an auction */
    @FXML
    public void onConnectWebSocket(ActionEvent event) {
        String auctionIdText = auctionIdField.getText();
        if (auctionIdText.isBlank()) {
            bidStatusLabel.setText("Enter an auction ID first.");
            return;
        }
        Long auctionId = Long.parseLong(auctionIdText);

        webSocketClient.connect(auctionId, message -> {
            // This runs on a background thread — must use Platform.runLater to touch UI
            Platform.runLater(() -> {
                // Parse: {"auctionId":1,"newHighestBid":500.00}
                String highest = message
                    .replaceAll(".*\"newHighestBid\":(\\d+\\.\\d+).*", "$1");
                liveHighestBidLabel.setText("Live highest bid: ₹" + highest);
            });
        });

        bidStatusLabel.setText("Connected to auction " + auctionId + " live feed.");
    }

    /** REST call to place a bid */
    @FXML
    public void onPlaceBid(ActionEvent event) {
        try {
            Long auctionId = Long.parseLong(auctionIdField.getText());
            Long buyerId   = Long.parseLong(buyerIdField.getText());
            Double amount  = Double.parseDouble(bidAmountField.getText());

            bidStatusLabel.setText("Placing bid...");

            new Thread(() -> {
                try {
                    JsonNode result = bidRestService.placeBid(auctionId, buyerId, amount);
                    Platform.runLater(() ->
                        bidStatusLabel.setText("Bid placed! Status: "
                            + result.get("status").asText())
                    );
                } catch (Exception e) {
                    Platform.runLater(() ->
                        bidStatusLabel.setText("Error: " + e.getMessage())
                    );
                }
            }).start();

        } catch (NumberFormatException e) {
            bidStatusLabel.setText("Please enter valid numbers in all fields.");
        }
    }

    @FXML
    public void onLogout(ActionEvent event) {
        if (webSocketClient != null) webSocketClient.disconnect();
        try {
            CampusApp.getInstance().loadLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}