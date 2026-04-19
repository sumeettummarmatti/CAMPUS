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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label roleLabel;
    @FXML private Button toggleRoleBtn;
    @FXML private TabPane mainTabs;
    @FXML private Tab buyerTab;
    @FXML private Tab sellerTab;
    @FXML private Tab notifTab;

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
    @FXML private Label createStatusLabel;
    @FXML private ListView<String> myAuctionListView;

    // Date pickers for auction start/end
    @FXML private DatePicker newStartDate;
    @FXML private ComboBox<Integer> newStartHour;
    @FXML private ComboBox<Integer> newStartMin;
    @FXML private DatePicker newEndDate;
    @FXML private ComboBox<Integer> newEndHour;
    @FXML private ComboBox<Integer> newEndMin;

    // Notification controls
    @FXML private ListView<String> notifListView;
    @FXML private Label notifStatusLabel;

    private BidRestService bidRestService;
    private AuctionRestService auctionRestService;
    private BidWebSocketClient webSocketClient;
    private User currentUser;
    private boolean viewingAsSeller = false;

    private static final DateTimeFormatter ISO_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @FXML
    public void initialize() {
        currentUser = CampusApp.getInstance().getCurrentUser();
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        bidRestService = new BidRestService(token);
        auctionRestService = new AuctionRestService(token);
        webSocketClient = new BidWebSocketClient();

        // Populate hour and minute combos (0–23, 0–55 step 5)
        List<Integer> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) hours.add(h);
        List<Integer> minutes = new ArrayList<>();
        for (int m = 0; m < 60; m += 5) minutes.add(m);

        newStartHour.setItems(FXCollections.observableArrayList(hours));
        newStartMin.setItems(FXCollections.observableArrayList(minutes));
        newEndHour.setItems(FXCollections.observableArrayList(hours));
        newEndMin.setItems(FXCollections.observableArrayList(minutes));

        // Sensible defaults: start = now+1h, end = now+2h
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultStart = now.plusHours(1);
        LocalDateTime defaultEnd   = now.plusHours(2);

        newStartDate.setValue(defaultStart.toLocalDate());
        newStartHour.setValue(defaultStart.getHour());
        newStartMin.setValue(roundToNearest5(defaultStart.getMinute()));

        newEndDate.setValue(defaultEnd.toLocalDate());
        newEndHour.setValue(defaultEnd.getHour());
        newEndMin.setValue(roundToNearest5(defaultEnd.getMinute()));

        // Auto-refresh notifications when the tab is selected
        notifTab.setOnSelectionChanged(e -> {
            if (notifTab.isSelected()) onRefreshNotifications(null);
        });

        updateRoleDisplay();
        onRefreshAuctions(null);
    }

    /** Round minutes up to the nearest multiple of 5 for default combo selection. */
    private int roundToNearest5(int m) {
        return (m / 5) * 5;
    }

    private void updateRoleDisplay() {
        String role = currentUser.getRole();
        roleLabel.setText("Logged in as: " + currentUser.getEmail() + " (" + role + ")");

        if (viewingAsSeller) {
            mainTabs.getSelectionModel().select(sellerTab);
            toggleRoleBtn.setText("Switch to Buyer");
        } else {
            mainTabs.getSelectionModel().select(buyerTab);
            toggleRoleBtn.setText("Switch to Seller");
        }
        sellerTab.setDisable(false);
    }

    @FXML
    public void onToggleRole(ActionEvent e) {
        viewingAsSeller = !viewingAsSeller;
        updateRoleDisplay();
        if (viewingAsSeller) onRefreshMyAuctions(null);
    }

    @FXML
    public void onRefreshAuctions(ActionEvent e) {
        new Thread(() -> {
            try {
                JsonNode auctions = auctionRestService.getActiveAuctions();
                ObservableList<String> items = FXCollections.observableArrayList();
                for (JsonNode a : auctions) {
                    long auctionId = a.path("id").asLong();
                    double displayPrice = a.path("price").asDouble();
                    try {
                        JsonNode bids = bidRestService.getBidsForAuction(auctionId);
                        if (bids.isArray() && bids.size() > 0) {
                            displayPrice = bids.get(0).path("amount").asDouble();
                        }
                    } catch (Exception ignore) {}

                    items.add(String.format("[ID:%d] %s — ₹%.0f  (%s)",
                        auctionId,
                        a.path("title").asText(),
                        displayPrice,
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
            // Validate required fields
            if (newStartDate.getValue() == null || newEndDate.getValue() == null) {
                createStatusLabel.setStyle("-fx-text-fill: red;");
                createStatusLabel.setText("Please select start and end dates.");
                return;
            }
            if (newStartHour.getValue() == null || newStartMin.getValue() == null
                    || newEndHour.getValue() == null || newEndMin.getValue() == null) {
                createStatusLabel.setStyle("-fx-text-fill: red;");
                createStatusLabel.setText("Please select hours and minutes for start and end times.");
                return;
            }

            String title   = newTitleField.getText();
            String desc    = newDescField.getText();
            double price   = Double.parseDouble(newPriceField.getText());
            double reserve = Double.parseDouble(newReserveField.getText());

            // Build ISO datetime from date picker + hour/minute combos
            LocalDate startDate = newStartDate.getValue();
            LocalDate endDate   = newEndDate.getValue();
            LocalDateTime startDT = LocalDateTime.of(
                startDate, java.time.LocalTime.of(newStartHour.getValue(), newStartMin.getValue()));
            LocalDateTime endDT   = LocalDateTime.of(
                endDate, java.time.LocalTime.of(newEndHour.getValue(), newEndMin.getValue()));

            String start = startDT.format(ISO_FMT);
            String end   = endDT.format(ISO_FMT);

            Long sellerId = currentUser.getId();
            createStatusLabel.setStyle("");
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
            createStatusLabel.setStyle("-fx-text-fill: red;");
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
            // Also refresh notifications on any live bid update
            onRefreshNotifications(null);
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

    /** Refresh the Notifications tab from the user's bid history. */
    @FXML
    public void onRefreshNotifications(ActionEvent e) {
        if (currentUser.getId() == null) return;
        new Thread(() -> {
            try {
                JsonNode bids = bidRestService.getMyBids(currentUser.getId());
                ObservableList<String> items = FXCollections.observableArrayList();

                if (bids.isArray()) {
                    for (JsonNode bid : bids) {
                        String status = bid.path("status").asText();
                        long auctionId = bid.path("auctionId").asLong();
                        double amount  = bid.path("amount").asDouble();

                        String line = switch (status) {
                            case "OUTBID"  -> String.format("🔔 You were OUTBID on Auction #%d — your bid was ₹%.0f", auctionId, amount);
                            case "WON"     -> String.format("🏆 You WON Auction #%d with a bid of ₹%.0f — check Profile for payment", auctionId, amount);
                            case "LOST"    -> String.format("❌ You did not win Auction #%d (your bid: ₹%.0f)", auctionId, amount);
                            case "LEADING" -> String.format("🟢 You are currently LEADING Auction #%d at ₹%.0f", auctionId, amount);
                            case "ACCEPTED"-> String.format("✅ Bid ACCEPTED on Auction #%d — ₹%.0f", auctionId, amount);
                            default        -> String.format("[%s] Auction #%d — ₹%.0f", status, auctionId, amount);
                        };
                        items.add(line);
                    }
                }

                Platform.runLater(() -> {
                    notifListView.setItems(items);
                    notifStatusLabel.setText(items.isEmpty()
                        ? "No bid activity yet. Place a bid to see notifications here."
                        : "Showing " + items.size() + " notification(s). Refresh to update.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> notifStatusLabel.setText("Error loading notifications: " + ex.getMessage()));
            }
        }).start();
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