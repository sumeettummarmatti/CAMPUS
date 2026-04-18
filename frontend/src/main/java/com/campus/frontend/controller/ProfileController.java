package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import com.campus.frontend.service.PaymentRestService;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProfileController {

    @FXML private Label userInfoLabel;
    @FXML private Label earningsLabel;
    @FXML private Label spendingLabel;
    @FXML private ListView<String> earningsListView;
    @FXML private ListView<String> spendingListView;
    @FXML private TextField confirmTxIdField;
    @FXML private TextField disputeReasonField;
    @FXML private Label actionStatusLabel;

    private PaymentRestService paymentRestService;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = CampusApp.getInstance().getCurrentUser();
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        paymentRestService = new PaymentRestService(token);

        userInfoLabel.setText("Email: " + currentUser.getEmail() + "  |  Role: " + currentUser.getRole());
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
                JsonNode earnings = paymentRestService.getEarnings(currentUser.getId());
                JsonNode spending = paymentRestService.getSpending(currentUser.getId());

                double totalEarned = 0, totalSpent = 0;
                ObservableList<String> earnItems = FXCollections.observableArrayList();
                ObservableList<String> spendItems = FXCollections.observableArrayList();

                for (JsonNode tx : earnings) {
                    double amt = tx.path("amount").asDouble();
                    totalEarned += (tx.path("status").asText().equals("COMPLETED")) ? amt : 0;
                    earnItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                        tx.path("id").asLong(),
                        tx.path("auctionId").asLong(),
                        amt,
                        tx.path("status").asText()));
                }

                for (JsonNode tx : spending) {
                    double amt = tx.path("amount").asDouble();
                    String status = tx.path("status").asText();
                    if (!status.equals("CANCELLED") && !status.equals("REFUNDED")) totalSpent += amt;
                    spendItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                        tx.path("id").asLong(),
                        tx.path("auctionId").asLong(),
                        amt,
                        status));
                }

                final double finalEarned = totalEarned;
                final double finalSpent  = totalSpent;

                Platform.runLater(() -> {
                    earningsLabel.setText(String.format("₹%.2f", finalEarned));
                    spendingLabel.setText(String.format("₹%.2f", finalSpent));
                    earningsListView.setItems(earnItems);
                    spendingListView.setItems(spendItems);
                });

            } catch (Exception e) {
                Platform.runLater(() -> actionStatusLabel.setText("Error loading payments: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onConfirmDelivery(ActionEvent e) {
        try {
            Long txId = Long.parseLong(confirmTxIdField.getText());
            new Thread(() -> {
                try {
                    paymentRestService.confirmDelivery(txId);
                    Platform.runLater(() -> {
                        actionStatusLabel.setStyle("-fx-text-fill: green;");
                        actionStatusLabel.setText("Delivery confirmed for TX#" + txId);
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
}