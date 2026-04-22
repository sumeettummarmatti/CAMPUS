package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import com.campus.frontend.service.PaymentRestService;
import com.campus.frontend.service.UserRestService;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class ProfileController {

    @FXML private Label userInfoLabel;
    @FXML private Label balanceLabel;
    @FXML private Label earningsLabel;
    @FXML private Label spendingLabel;
    @FXML private CheckBox walletModeCheck;
    @FXML private CheckBox gpayModeCheck;
    @FXML private CheckBox cardModeCheck;
    @FXML private CheckBox cashModeCheck;
    @FXML private CheckBox netBankModeCheck;
    @FXML private ListView<String> earningsListView;
    @FXML private ListView<String> spendingListView;
    @FXML private TextField confirmTxIdField;
    @FXML private TextField disputeReasonField;
    @FXML private TextField topUpAmountField;
    @FXML private Label actionStatusLabel;
    @FXML private TextField sellerTxIdField;

    private PaymentRestService paymentRestService;
    private UserRestService userRestService;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = CampusApp.getInstance().getCurrentUser();
        userRestService = CampusApp.getInstance().getRestService();
        String token = userRestService.getAuthToken();
        paymentRestService = new PaymentRestService(token);

        userInfoLabel.setText("Email: " + currentUser.getEmail() + "  |  Role: " + currentUser.getRole());
        loadWalletModes();
        loadPaymentData();
    }

    private void loadPaymentData() {
        if (currentUser.getId() == null) {
            earningsLabel.setText("₹0.00");
            spendingLabel.setText("₹0.00");
            return;
        }

        new Thread(() -> {
            try {
                User latest = userRestService.fetchCurrentUserProfile();
                currentUser.setWalletBalance(latest.getWalletBalance());
                currentUser.setTotalEarned(latest.getTotalEarned());
                currentUser.setTotalSpent(latest.getTotalSpent());
                currentUser.setEnabledPaymentModes(latest.getEnabledPaymentModes());

                JsonNode earnings = paymentRestService.getEarnings(currentUser.getId());
                JsonNode spending = paymentRestService.getSpending(currentUser.getId());

                // DEBUG — print what we got
                System.out.println("[Profile] Earnings response: " + earnings.toString());
                System.out.println("[Profile] Spending response: " + spending.toString());
                System.out.println("[Profile] Seller ID: " + currentUser.getId());

                ObservableList<String> earnItems = FXCollections.observableArrayList();
                ObservableList<String> spendItems = FXCollections.observableArrayList();

                if (earnings.isArray()) {
                    for (JsonNode tx : earnings) {
                        earnItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                            tx.path("id").asLong(),
                            tx.path("auctionId").asLong(),
                            tx.path("amount").asDouble(),
                            tx.path("status").asText()));
                    }
                } else {
                    System.out.println("[Profile] Earnings was not an array: "
                        + earnings.toString());
                }

                if (spending.isArray()) {
                    for (JsonNode tx : spending) {
                        spendItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                            tx.path("id").asLong(),
                            tx.path("auctionId").asLong(),
                            tx.path("amount").asDouble(),
                            tx.path("status").asText()));
                    }
                }

                Platform.runLater(() -> {
                    balanceLabel.setText(
                        String.format("₹%.2f", currentUser.getWalletBalance()));
                    earningsLabel.setText(
                        String.format("₹%.2f", currentUser.getTotalEarned()));
                    spendingLabel.setText(
                        String.format("₹%.2f", currentUser.getTotalSpent()));
                    earningsListView.setItems(earnItems);
                    spendingListView.setItems(spendItems);

                    if (earnItems.isEmpty()) {
                        earningsListView.setPlaceholder(new Label(
                            "No sales yet. Complete an auction sale to see it here."));
                    }
                    if (spendItems.isEmpty()) {
                        spendingListView.setPlaceholder(new Label(
                            "No purchases yet."));
                    }

                    applyWalletModes(currentUser.getEnabledPaymentModes());
                });

            } catch (Exception e) {
                System.out.println("[Profile] ERROR loading payment data: "
                    + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() ->
                    actionStatusLabel.setText(
                        "Error loading payments: " + e.getMessage()));
            }
        }).start();
    }
    
    private void loadWalletModes() {
        if (currentUser.getId() == null) {
            return;
        }
        new Thread(() -> {
            try {
                List<String> modes = userRestService.getWalletModes(currentUser.getId());
                currentUser.setEnabledPaymentModes(modes);
                Platform.runLater(() -> applyWalletModes(modes));
            } catch (Exception ex) {
                Platform.runLater(() -> actionStatusLabel.setText("Could not load wallet modes: " + ex.getMessage()));
            }
        }).start();
    }

    private void applyWalletModes(List<String> modes) {
        walletModeCheck.setSelected(modes.contains("CAMPUS_WALLET"));
        gpayModeCheck.setSelected(modes.contains("GOOGLE_PAY") || modes.contains("UPI"));
        cardModeCheck.setSelected(modes.contains("CARD") || modes.contains("CREDIT_CARD"));
        cashModeCheck.setSelected(modes.contains("CASH") || modes.contains("CASH_ON_DELIVERY"));
        netBankModeCheck.setSelected(modes.contains("NET_BANKING"));
    }

    @FXML
    public void onTopUpWallet(ActionEvent e) {
        if (currentUser.getId() == null) return;
        String amountText = topUpAmountField.getText();
        if (amountText.isBlank()) {
            actionStatusLabel.setText("Enter an amount to recharge.");
            return;
        }
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                actionStatusLabel.setText("Enter a positive amount.");
                return;
            }
            new Thread(() -> {
                try {
                    userRestService.topUpWallet(currentUser.getId(), amount);
                    Platform.runLater(() -> {
                        actionStatusLabel.setStyle("-fx-text-fill: green;");
                        actionStatusLabel.setText("Wallet recharged successfully: ₹" + amount);
                        topUpAmountField.clear();
                        loadPaymentData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> actionStatusLabel.setText("Recharge failed: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            actionStatusLabel.setText("Enter a valid numeric amount.");
        }
    }

    @FXML
    public void onSaveWalletModes(ActionEvent e) {
        if (currentUser.getId() == null) {
            actionStatusLabel.setText("No user loaded.");
            return;
        }
        List<String> modes = new ArrayList<>();
        if (walletModeCheck.isSelected()) modes.add("CAMPUS_WALLET");
        if (gpayModeCheck.isSelected()) modes.add("GOOGLE_PAY");
        if (cardModeCheck.isSelected()) modes.add("CARD");
        if (cashModeCheck.isSelected()) modes.add("CASH");
        if (netBankModeCheck.isSelected()) modes.add("NET_BANKING");
        if (modes.isEmpty()) {
            actionStatusLabel.setText("Select at least one payment mode.");
            return;
        }
        new Thread(() -> {
            try {
                List<String> updated = userRestService.updateWalletModes(currentUser.getId(), modes);
                currentUser.setEnabledPaymentModes(updated);
                Platform.runLater(() -> {
                    actionStatusLabel.setStyle("-fx-text-fill: green;");
                    actionStatusLabel.setText("Wallet modes saved.");
                    applyWalletModes(updated);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> actionStatusLabel.setText("Could not save modes: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onConfirmDelivery(ActionEvent e) {
        try {
            Long txId = Long.parseLong(confirmTxIdField.getText());
            new Thread(() -> {
                try {
                    // Step 1: confirm delivery (SHIPPED → DELIVERY_CONFIRMED)
                    paymentRestService.confirmDelivery(txId);
                    // Step 2: release funds to seller (DELIVERY_CONFIRMED → COMPLETED)
                    paymentRestService.releaseFunds(txId);
                    Platform.runLater(() -> {
                        actionStatusLabel.setStyle("-fx-text-fill: green;");
                        actionStatusLabel.setText(
                            "✅ Delivery confirmed! Funds released to seller. TX#" + txId);
                        loadPaymentData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> actionStatusLabel.setText("Error: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            actionStatusLabel.setText("Enter a valid transaction ID.");
        }
    }

    @FXML
    public void onOpenDispute(ActionEvent e) {
        try {
            Long txId = Long.parseLong(confirmTxIdField.getText());
            String reason = disputeReasonField.getText();
            if (reason.isBlank()) { actionStatusLabel.setText("Enter a reason for the dispute."); return; }
            new Thread(() -> {
                try {
                    paymentRestService.openDispute(txId, reason);
                    Platform.runLater(() -> {
                        actionStatusLabel.setStyle("-fx-text-fill: orange;");
                        actionStatusLabel.setText("Dispute opened for TX#" + txId);
                        loadPaymentData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> actionStatusLabel.setText("Error: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            actionStatusLabel.setText("Enter a valid transaction ID.");
        }
    }

    @FXML
    public void onBack(ActionEvent e) {
        try { CampusApp.getInstance().loadDashboard(); } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    public void onMarkShipped(ActionEvent e) {
        try {
            Long txId = Long.parseLong(sellerTxIdField.getText());
            new Thread(() -> {
                try {
                    paymentRestService.markShipped(txId);
                    Platform.runLater(() -> {
                        actionStatusLabel.setStyle("-fx-text-fill: green;");
                        actionStatusLabel.setText(
                            "✅ TX#" + txId + " marked as Shipped! " +
                            "Buyer will now be asked to confirm delivery.");
                        loadPaymentData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> actionStatusLabel.setText("Error: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            actionStatusLabel.setText("Enter a valid transaction ID.");
        }
    }

    @FXML
    public void onRequestSellerAccess(ActionEvent e) {
        if (currentUser.getId() == null) return;
        new Thread(() -> {
            try {
                HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(
                            com.campus.frontend.config.AppConfig.userServiceUrl() +
                            "/api/users/" + currentUser.getId() + "/verify/request"))
                        .header("Authorization", "Bearer " + userRestService.getAuthToken())
                        .POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
                java.net.http.HttpClient.newHttpClient()
                        .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    actionStatusLabel.setStyle("-fx-text-fill: green;");
                    actionStatusLabel.setText(
                        "✅ Seller access requested! An admin will review your account. " +
                        "You'll be able to list items once approved.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> actionStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }
}
