package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import com.campus.frontend.service.AuctionRestService;
import com.campus.frontend.service.BidRestService;
import com.campus.frontend.service.BidWebSocketClient;
import com.campus.frontend.service.PaymentRestService;
import com.campus.frontend.service.UserRestService;
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
    @FXML private Label countdownTimerLabel; // NEW
    @FXML private Label liveClockLabel;     // NEW

    // Seller controls
    @FXML private TextField newTitleField;
    @FXML private TextField newDescField;
    @FXML private TextField newPriceField;
    @FXML private TextField newReserveField;
    @FXML private Label createStatusLabel;
    @FXML private ListView<String> myAuctionListView;
    @FXML private TextField terminateAuctionIdField;

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
    private PaymentRestService paymentRestService;
    private UserRestService userRestService;
    private User currentUser;
    private boolean viewingAsSeller = false;
    private String globalAnnouncements = "";
    private LocalDateTime selectedAuctionEndTime; // NEW
    private Long currentlyWatchedAuctionId;      // NEW
    private javafx.animation.Timeline timerTimeline; // NEW
    private final java.util.Set<Long> handledEndedAuctions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean paymentPopupOpen = false;

    private static final DateTimeFormatter ISO_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @FXML
    public void initialize() {
        currentUser = CampusApp.getInstance().getCurrentUser();
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        bidRestService = new BidRestService(token);
        auctionRestService = new AuctionRestService(token);
        webSocketClient = new BidWebSocketClient();
        paymentRestService = new PaymentRestService(token);
        userRestService = CampusApp.getInstance().getRestService();

        // Populate hour and minute combos (0–23, 0–55 step 5)
        List<Integer> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) hours.add(h);
        List<Integer> minutes = new ArrayList<>();
        for (int m = 0; m < 60; m++) minutes.add(m);

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
        newStartMin.setValue(defaultStart.getMinute());

        newEndDate.setValue(defaultEnd.toLocalDate());
        newEndHour.setValue(defaultEnd.getHour());
        newEndMin.setValue(defaultEnd.getMinute());

        // Auto-refresh notifications when the tab is selected
        notifTab.setOnSelectionChanged(e -> {
            if (notifTab.isSelected()) onRefreshNotifications(null);
        });

        updateRoleDisplay();
        onRefreshAuctions(null);
        showGlobalAnnouncements();
        connectGlobalWinnerChannel();
        startTimerTimeline();

        // Double-click on active auction -> show detail popup
        auctionListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = auctionListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.startsWith("[ID:")) {
                    try {
                        long auctionId = Long.parseLong(selected.substring(4, selected.indexOf(']')));
                        openAuctionDetailById(auctionId);
                    } catch (Exception ignored) {}
                }
            }
        });

        // Double-click on my (seller) auction -> show detail popup
        myAuctionListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = myAuctionListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.startsWith("[ID:")) {
                    try {
                        long auctionId = Long.parseLong(selected.substring(4, selected.indexOf(']')));
                        openAuctionDetailById(auctionId);
                    } catch (Exception ignored) {}
                }
            }
        });

        // Show wallet balance hint
        if (currentUser.getWalletBalance() > 0) {
            bidStatusLabel.setStyle("-fx-text-fill: #64748b;");
            bidStatusLabel.setText(
                "Wallet: ₹" + String.format("%.2f", currentUser.getWalletBalance()) +
                "  |  Payment modes: " +
                (currentUser.getEnabledPaymentModes().isEmpty()
                    ? "⚠️ None set — go to Profile"
                    : String.join(", ", currentUser.getEnabledPaymentModes())));
        }
    }

    private void startTimerTimeline() {
        timerTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                updateLiveClock();
                updateCountdown();
            })
        );
        timerTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateLiveClock() {
        if (liveClockLabel != null) {
            liveClockLabel.setText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void updateCountdown() {
        if (countdownTimerLabel == null || selectedAuctionEndTime == null) return;
        
        java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), selectedAuctionEndTime);
        if (diff.isNegative() || diff.isZero()) {
            countdownTimerLabel.setText("Ended");
            selectedAuctionEndTime = null;
        } else {
            long s = diff.getSeconds();
            countdownTimerLabel.setText(String.format("Time Left: %02d:%02d", s / 60, s % 60));
        }
    }

    private void connectGlobalWinnerChannel() {
        webSocketClient.connect(0L, message -> Platform.runLater(() -> {
            try {
                JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(message);
                String type = root.path("type").asText();
                if ("AUCTION_ENDED".equals(type)) {
                    long auctionId = root.path("auctionId").asLong();
                    String winnerName = root.path("winnerName").asText();
                    long winnerId = root.path("winnerId").asLong(-1L);
                    double amount = root.path("amount").asDouble();

                    String announcement = String.format(
                        "🏆 AUCTION ENDED: Auction #%d won by %s for ₹%.2f",
                        auctionId, winnerName, amount);
                    notifListView.getItems().add(0, announcement);
                    onRefreshAuctions(null);

                    // Show payment popup if this buyer won, even if they weren't watching live
                    boolean iWon = winnerId == currentUser.getId()
                        || (currentUser.getEmail() != null
                            && currentUser.getEmail().equalsIgnoreCase(winnerName));
                    boolean alreadyHandled = handledEndedAuctions.contains(auctionId);

                    if (iWon && !alreadyHandled && !paymentPopupOpen) {
                        handledEndedAuctions.add(auctionId);
                        openPaymentOptions(
                            auctionId,
                            root.path("sellerId").asLong(),
                            root.path("title").asText(),
                            amount
                        );
                    }
                } else if ("GLOBAL_ANNOUNCEMENT".equals(type)) {
                    String msg = root.path("message").asText();
                    notifListView.getItems().add(0, msg);
                    if (msg.contains("ACTIVE")) {
                        onRefreshAuctions(null);
                    }
                }
            } catch (Exception e) {
                // Ignore malformed messages
            }
        }));
    }
    
    private void showGlobalAnnouncements() {
        new Thread(() -> {
            try {
                JsonNode active = auctionRestService.getActiveAuctions();
                JsonNode scheduled = auctionRestService.getScheduledAuctions();
                
                StringBuilder sb = new StringBuilder();
                if (active != null && active.size() > 0) {
                    sb.append("🔥 ACTIVE AUCTIONS:\n");
                    for (JsonNode a : active) {
                        sb.append(String.format(" - ID %d: '%s' (₹%.0f)\n", a.path("id").asLong(), a.path("title").asText(), a.path("price").asDouble()));
                    }
                    sb.append("\n");
                }
                if (scheduled != null && scheduled.size() > 0) {
                    sb.append("⏳ UPCOMING SCHEDULED AUCTIONS:\n");
                    for (JsonNode a : scheduled) {
                        // Formats the timestamp nicely if available
                        String rawTime = a.path("startTime").asText();
                        sb.append(String.format(" - ID %d: '%s' starting at %s\n", a.path("id").asLong(), a.path("title").asText(), rawTime));
                    }
                }
                
                if (sb.length() > 0) {
                    globalAnnouncements = sb.toString().trim();
                    Platform.runLater(() -> onRefreshNotifications(null));
                }
            } catch (Exception e) {}
        }).start();
    }

    private void updateRoleDisplay() {
        roleLabel.setText("Logged in as: " + currentUser.getEmail());

        boolean isVerifiedSeller = "SELLER".equals(currentUser.getRole()) && currentUser.isVerified();

        // Only verified sellers get the toggle button
        toggleRoleBtn.setVisible(isVerifiedSeller);
        toggleRoleBtn.setManaged(isVerifiedSeller);

        if (isVerifiedSeller && viewingAsSeller) {
            if (!mainTabs.getTabs().contains(sellerTab)) {
                mainTabs.getTabs().add(1, sellerTab);
            }
            mainTabs.getSelectionModel().select(sellerTab);
            toggleRoleBtn.setText("Switch to Buyer");
        } else {
            mainTabs.getTabs().remove(sellerTab);
            mainTabs.getSelectionModel().select(buyerTab);
            if (isVerifiedSeller) {
                toggleRoleBtn.setText("Switch to Seller");
            }
        }
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
                    long sellerId  = a.path("sellerId").asLong();
                    double displayPrice = a.path("price").asDouble();

                    // Fetch live highest bid
                    try {
                        JsonNode bids = bidRestService.getBidsForAuction(auctionId);
                        if (bids.isArray() && bids.size() > 0) {
                            displayPrice = bids.get(0).path("amount").asDouble();
                        }
                    } catch (Exception ignore) {}

                    // Check if seller is verified
                    boolean sellerVerified =
                        userRestService.isUserVerified(sellerId);
                    String verifiedBadge = sellerVerified
                        ? " [✓ Verified Seller]"
                        : " [Unverified Seller]";

                    items.add(String.format("[ID:%d] %s — ₹%.0f  (%s)%s",
                        auctionId,
                        a.path("title").asText(),
                        displayPrice,
                        a.path("status").asText(),
                        verifiedBadge));
                }
                Platform.runLater(() -> auctionListView.setItems(items));
            } catch (Exception ex) {
                Platform.runLater(() ->
                    bidStatusLabel.setText(
                        "Could not load auctions: " + ex.getMessage()));
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
                    long auctionId = a.path("id").asLong();
                    double reserve = a.path("reservePrice").asDouble();
                    double highest = 0;
                    try {
                        JsonNode bids = bidRestService.getBidsForAuction(auctionId);
                        if (bids.isArray() && bids.size() > 0) {
                            highest = bids.get(0).path("amount").asDouble();
                        }
                    } catch (Exception ignore) {}

                    String reserveInfo = (highest >= reserve && reserve > 0) ? " [RESERVE MET!]" : " [Reserve: ₹" + reserve + "]";
                    items.add(String.format("[ID:%d] %s — ₹%.0f / ₹%.0f%s (%s)",
                        auctionId,
                        a.path("title").asText(),
                        highest,
                        reserve,
                        reserveInfo,
                        a.path("status").asText()));
                }
                Platform.runLater(() -> myAuctionListView.setItems(items));
            } catch (Exception ex) {
                Platform.runLater(() -> createStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onTerminateEarly(ActionEvent e) {
        String idText = terminateAuctionIdField.getText();
        if (idText.isBlank()) { createStatusLabel.setText("Enter ID to terminate."); return; }
        try {
            Long auctionId = Long.parseLong(idText);
            createStatusLabel.setText("Terminating auction #" + auctionId + "...");
            new Thread(() -> {
                try {
                    auctionRestService.terminateAuction(auctionId, currentUser.getId());
                    // Trigger resolve in bidding service
                    bidRestService.resolveAuction(auctionId);
                    Platform.runLater(() -> {
                        createStatusLabel.setStyle("-fx-text-fill: green;");
                        createStatusLabel.setText("Auction terminated successfully!");
                        onRefreshMyAuctions(null);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        createStatusLabel.setStyle("-fx-text-fill: red;");
                        createStatusLabel.setText("Termination failed: " + ex.getMessage());
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            createStatusLabel.setText("Invalid ID format.");
        }
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
            if (newStartHour.getSelectionModel().isEmpty() || newStartMin.getSelectionModel().isEmpty()
                    || newEndHour.getSelectionModel().isEmpty() || newEndMin.getSelectionModel().isEmpty()) {
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

            // Check payment mode is set
            java.util.List<String> modes = currentUser.getEnabledPaymentModes();
            if (modes == null || modes.isEmpty()) {
                createStatusLabel.setStyle("-fx-text-fill: #e67e22;");
                createStatusLabel.setText(
                    "⚠️ Set up a payment mode in Profile first before listing items.");
                return;
            }

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
        webSocketHandlerClientConnect(auctionId);
    }

    private void webSocketHandlerClientConnect(Long auctionId) {
        webSocketClient.disconnect();
        this.currentlyWatchedAuctionId = auctionId;
        // Fetch end time first so timer works
        new Thread(() -> {
            try {
                JsonNode a = auctionRestService.getActiveAuctions(); // Ideally getById
                // Find our auction in the list for now to get endTime
                for (JsonNode node : a) {
                    if (node.path("id").asLong() == auctionId) {
                        String et = node.path("endTime").asText();
                        Platform.runLater(() -> selectedAuctionEndTime = LocalDateTime.parse(et));
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }).start();

        webSocketClient.connect(auctionId, message -> Platform.runLater(() -> {
            try {
                JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(message);
                String type = root.path("type").asText();
                if ("AUCTION_ENDED".equals(type)) {
                    long auctionIdFromEvent = root.path("auctionId").asLong();
                    String winnerName = root.path("winnerName").asText();
                    long winnerId = root.path("winnerId").asLong(-1L);
                    double amount = root.path("amount").asDouble();
                    liveHighestBidLabel.setText("🏆 Auction Ended! Winner: " + winnerName + " with bid ₹" + amount);
                    
                    // Auto-Redirect winner to payment
                    boolean iWon = winnerId == currentUser.getId() || winnerName.equalsIgnoreCase(currentUser.getEmail());
                    boolean alreadyHandled = handledEndedAuctions.contains(auctionIdFromEvent);
                    if (iWon && !alreadyHandled && !paymentPopupOpen) {
                        handledEndedAuctions.add(auctionIdFromEvent);
                        Platform.runLater(() -> openPaymentOptions(
                            root.path("auctionId").asLong(),
                            root.path("sellerId").asLong(),
                            root.path("title").asText(),
                            amount
                        ));
                    }
                } else {
                    double highest = root.path("newHighestBid").asDouble();
                    liveHighestBidLabel.setText("🔴 LIVE — Auction #" + auctionId + " highest bid: ₹" + highest);
                }
            } catch (Exception e) {
                // Handle non-json or old format
                if (message.contains("{")) { /* skip errors if trying to parse JSON */ }
                else {
                    liveHighestBidLabel.setText("🔴 LIVE — " + message);
                }
            }
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

            bidStatusLabel.setText("Checking your account...");

            new Thread(() -> {
                try {
                    // Always fetch latest profile for accurate balance
                    User latest = userRestService.fetchCurrentUserProfile();

                    // --- Check 1: Payment mode set up ---
                    java.util.List<String> modes = latest.getEnabledPaymentModes();
                    if (modes == null || modes.isEmpty()) {
                        Platform.runLater(() -> {
                            bidStatusLabel.setStyle("-fx-text-fill: #e67e22;");
                            bidStatusLabel.setText(
                                "⚠️ Set up a payment mode first!\n" +
                                "Go to Profile → Wallet Payment Modes → " +
                                "tick at least one option → Save Modes.");
                        });
                        return;
                    }

                    // --- Check 2: Minimum ₹100 in wallet ---
                    double balance = latest.getWalletBalance();
                    if (balance < 100.0) {
                        Platform.runLater(() -> {
                            bidStatusLabel.setStyle("-fx-text-fill: #e67e22;");
                            bidStatusLabel.setText(
                                "⚠️ Your wallet needs at least ₹100 to bid.\n" +
                                "Current balance: ₹" + String.format("%.2f", balance) +
                                "  →  Go to Profile → Add Funds.");
                        });
                        return;
                    }

                    // --- Check 3: Sufficient balance for this bid ---
                    if (amount > balance) {
                        Platform.runLater(() -> {
                            bidStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                            bidStatusLabel.setText(
                                "❌ Insufficient balance!\n" +
                                "Bid: ₹" + String.format("%.2f", amount) +
                                "  |  Your wallet: ₹" +
                                String.format("%.2f", balance) +
                                "  →  Go to Profile → Add Funds.");
                        });
                        return;
                    }

                    // All checks passed — place the bid
                    Platform.runLater(() -> {
                        bidStatusLabel.setStyle("");
                        bidStatusLabel.setText("Placing bid...");
                    });
                    JsonNode result = bidRestService.placeBid(auctionId, buyerId, amount);
                    Platform.runLater(() -> {
                        bidStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                        bidStatusLabel.setText(
                            "✅ Bid placed! Status: " +
                            result.get("status").asText());
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        bidStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                        bidStatusLabel.setText("❌ " + ex.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException ex) {
            bidStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            bidStatusLabel.setText(
                "Enter valid numbers for auction ID and bid amount.");
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
                
                if (!globalAnnouncements.isEmpty()) {
                    items.add(0, globalAnnouncements);
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

    /** Opens a detail popup for any auction by ID. */
    private void openAuctionDetailById(long auctionId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/AuctionDetail.fxml"));
            javafx.scene.Parent root = loader.load();
            AuctionDetailController ctrl = loader.getController();
            ctrl.loadAuctionById(auctionId);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Auction Details");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            bidStatusLabel.setText("Could not open detail: " + e.getMessage());
        }
    }

    private void openPaymentOptions(long aucId, long selId, String title, double amt) {
        try {
            if (paymentPopupOpen) {
                return;
            }
            paymentPopupOpen = true;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/PaymentOptions.fxml"));
            javafx.scene.Parent root = loader.load();
            PaymentOptionsController controller = loader.getController();
            controller.initData(aucId, selId, currentUser.getId(), amt, title, () -> onRefreshNotifications(null));
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Complete Your Purchase");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> paymentPopupOpen = false);
            stage.show();
        } catch (Exception e) {
            paymentPopupOpen = false;
            e.printStackTrace();
            bidStatusLabel.setText("Error opening payment screen: " + e.getMessage());
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
