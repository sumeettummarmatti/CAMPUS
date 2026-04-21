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

                ObservableList<String> earnItems = FXCollections.observableArrayList();
                ObservableList<String> spendItems = FXCollections.observableArrayList();

                for (JsonNode tx : earnings) {
                    double amt = tx.path("amount").asDouble();
                    earnItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                        tx.path("id").asLong(),
                        tx.path("auctionId").asLong(),
                        amt,
                        tx.path("status").asText()));
                }

                for (JsonNode tx : spending) {
                    double amt = tx.path("amount").asDouble();
                    String status = tx.path("status").asText();
                    spendItems.add(String.format("[TX#%d] Auction #%d — ₹%.2f — %s",
                        tx.path("id").asLong(),
                        tx.path("auctionId").asLong(),
                        amt,
                        status));
                }

                Platform.runLater(() -> {
                    balanceLabel.setText(String.format("₹%.2f", currentUser.getWalletBalance()));
                    earningsLabel.setText(String.format("₹%.2f", currentUser.getTotalEarned()));
                    spendingLabel.setText(String.format("₹%.2f", currentUser.getTotalSpent()));
                    earningsListView.setItems(earnItems);
                    spendingListView.setItems(spendItems);
                    applyWalletModes(currentUser.getEnabledPaymentModes());
                });

            } catch (Exception e) {
                Platform.runLater(() -> actionStatusLabel.setText("Error loading payments: " + e.getMessage()));
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
