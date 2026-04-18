package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import com.campus.frontend.service.AuctionRestService;
import com.campus.frontend.service.BidRestService;
import com.campus.frontend.service.BidWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DashboardController {

    @FXML private Label roleLabel;
    @FXML private Button toggleRoleBtn;
    @FXML private TabPane mainTabs;
    @FXML private Tab buyerTab;
    @FXML private Tab sellerTab;

    // Buyer controls
    @FXML private ListView<String> auctionListView;
    @FXML private TextField auctionIdField;
    @FXML private TextField bidAmountField;
    @FXML private Label bidStatusLabel;
    @FXML private Label liveHighestBidLabel;

    // Seller controls
    @FXML private TextField newTitleField;
    @FXML private TextField newDescField;
    @FXML private TextField newPriceField;
    @FXML private TextField newReserveField;
    @FXML private TextField newStartField;
    @FXML private TextField newEndField;
    @FXML private Label createStatusLabel;
    @FXML private ListView<String> myAuctionListView;

    private BidRestService bidRestService;
    private AuctionRestService auctionRestService;
    private BidWebSocketClient webSocketClient;
    private User currentUser;
    private boolean viewingAsSeller = false;

    @FXML
    public void initialize() {
        currentUser = CampusApp.getInstance().getCurrentUser();
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        bidRestService = new BidRestService(token);
        auctionRestService = new AuctionRestService(token);
        webSocketClient = new BidWebSocketClient();

        updateRoleDisplay();
        onRefreshAuctions(null);
    }

    private void updateRoleDisplay() {
        // Every user can do both — the toggle just switches the active tab
        String mode = viewingAsSeller ? "Seller mode" : "Buyer mode";
        roleLabel.setText(currentUser.getEmail() + " — " + mode);

        if (viewingAsSeller) {
            mainTabs.getSelectionModel().select(sellerTab);
            toggleRoleBtn.setText("Switch to Buyer mode");
        } else {
            mainTabs.getSelectionModel().select(buyerTab);
            toggleRoleBtn.setText("Switch to Seller mode");
        }

        // Never disable the seller tab — all users can sell
        sellerTab.setDisable(false);
    }

    @FXML
    public void onToggleRole(ActionEvent e) {
        viewingAsSeller = !viewingAsSeller;
        updateRoleDisplay();
        if (viewingAsSeller) onRefreshMyAuctions(null);
        else onRefreshAuctions(null);
    }

    @FXML
    public void onRefreshAuctions(ActionEvent e) {
        new Thread(() -> {
            try {
                JsonNode auctions = auctionRestService.getActiveAuctions();
                ObservableList<String> items = FXCollections.observableArrayList();
                for (JsonNode a : auctions) {
                    items.add(String.format("[ID:%d] %s — ₹%.0f  (%s)",
                        a.path("id").asLong(),
                        a.path("title").asText(),
                        a.path("price").asDouble(),
                        a.path("status").asText()));
                }
                Platform.runLater(() -> auctionListView.setItems(items));
            } catch (Exception ex) {
                Platform.runLater(() -> bidStatusLabel.setText("Could not load auctions: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRefreshMyAuctions(ActionEvent e) {
        if (currentUser.getId() == null) return;
        new Thread(() -> {
            try {
                JsonNode auctions = auctionRestService.getMyAuctions(currentUser.getId());
                ObservableList<String> items = FXCollections.observableArrayList();
                for (JsonNode a : auctions) {
                    items.add(String.format("[ID:%d] %s — %s",
                        a.path("id").asLong(),
                        a.path("title").asText(),
                        a.path("status").asText()));
                }
                Platform.runLater(() -> myAuctionListView.setItems(items));
            } catch (Exception ex) {
                Platform.runLater(() -> createStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onCreateAuction(ActionEvent e) {
        try {
            String title   = newTitleField.getText();
            String desc    = newDescField.getText();
            double price   = Double.parseDouble(newPriceField.getText());
            double reserve = Double.parseDouble(newReserveField.getText());
            String start   = newStartField.getText() + ":00"; // append seconds
            String end     = newEndField.getText() + ":00";
            Long sellerId  = currentUser.getId();

            createStatusLabel.setText("Creating…");

            new Thread(() -> {
                try {
                    JsonNode auction = auctionRestService.createAuction(
                        title, desc, price, reserve, start, end, sellerId);
                    Long auctionId = auction.path("id").asLong();
                    auctionRestService.scheduleAuction(auctionId);
                    Platform.runLater(() -> {
                        createStatusLabel.setStyle("-fx-text-fill: green;");
                        createStatusLabel.setText("Auction #" + auctionId + " created and scheduled!");
                        onRefreshMyAuctions(null);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        createStatusLabel.setStyle("-fx-text-fill: red;");
                        createStatusLabel.setText("Error: " + ex.getMessage());
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            createStatusLabel.setText("Please enter valid numbers for prices.");
        }
    }

    @FXML
    public void onConnectWebSocket(ActionEvent e) {
        String id = auctionIdField.getText();
        if (id.isBlank()) { bidStatusLabel.setText("Enter auction ID first."); return; }
        Long auctionId = Long.parseLong(id);
        webSocketClient.connect(auctionId, message -> Platform.runLater(() -> {
            String highest = message.replaceAll(".*\"newHighestBid\":(\\d+\\.?\\d*).*", "$1");
            liveHighestBidLabel.setText("🔴 LIVE — Auction #" + auctionId + " highest bid: ₹" + highest);
        }));
        bidStatusLabel.setText("Connected to live feed for auction #" + auctionId);
    }

    @FXML
    public void onPlaceBid(ActionEvent e) {
        try {
            Long auctionId = Long.parseLong(auctionIdField.getText());
            Double amount  = Double.parseDouble(bidAmountField.getText());
            Long buyerId   = currentUser.getId();

            bidStatusLabel.setText("Placing bid…");
            new Thread(() -> {
                try {
                    JsonNode result = bidRestService.placeBid(auctionId, buyerId, amount);
                    Platform.runLater(() -> bidStatusLabel.setText(
                        "✅ Bid placed! Status: " + result.get("status").asText()));
                } catch (Exception ex) {
                    Platform.runLater(() -> bidStatusLabel.setText("❌ " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            bidStatusLabel.setText("Enter valid numbers for auction ID and amount.");
        }
    }

    @FXML
    public void onProfile(ActionEvent e) {
        try { CampusApp.getInstance().loadProfileScreen(); } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    public void onLogout(ActionEvent e) {
        if (webSocketClient != null) webSocketClient.disconnect();
        try { CampusApp.getInstance().loadLoginScreen(); } catch (Exception ex) { ex.printStackTrace(); }
    }
}