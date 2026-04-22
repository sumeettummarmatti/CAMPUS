package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.service.AdminRestService;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminDashboardController {

    @FXML private Label adminEmailLabel;
    @FXML private ListView<String> pendingSellersListView;
    @FXML private TextField verifyUserIdField;
    @FXML private Label verifyStatusLabel;
    @FXML private ListView<String> allUsersListView;
    @FXML private TextField disputeTxIdField;
    @FXML private Label disputeStatusLabel;

    private AdminRestService adminRestService;

    @FXML
    public void initialize() {
        String token = CampusApp.getInstance().getRestService().getAuthToken();
        adminRestService = new AdminRestService(token);
        adminEmailLabel.setText("Admin: " + CampusApp.getInstance().getCurrentUser().getEmail());
        onRefreshUsers(null);
        onRefreshPendingSellers(null);
    }

    @FXML
    public void onRefreshPendingSellers(ActionEvent e) {
        new Thread(() -> {
            try {
                JsonNode users = adminRestService.getAllUsers();
                ObservableList<String> items = FXCollections.observableArrayList();
                for (JsonNode u : users) {
                    String role = u.path("role").asText();
                    boolean verified = u.path("verified").asBoolean();
                    // Show ALL unverified users except admins
                    if (!verified && !"ADMIN".equals(role)) {
                        items.add(String.format(
                            "[ID:%d] %s (%s) — Role: %s — AWAITING APPROVAL",
                            u.path("id").asLong(),
                            u.path("fullName").asText(),
                            u.path("email").asText(),
                            role));
                    }
                }
                Platform.runLater(() -> {
                    pendingSellersListView.setItems(items);
                    pendingSellersListView.setPlaceholder(
                        new Label("No pending approvals."));
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                    verifyStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRefreshUsers(ActionEvent e) {
        new Thread(() -> {
            try {
                JsonNode users = adminRestService.getAllUsers();
                ObservableList<String> items = FXCollections.observableArrayList();
                for (JsonNode u : users) {
                    items.add(String.format("[ID:%d] %s — %s — Role: %s — Verified: %s",
                        u.path("id").asLong(),
                        u.path("fullName").asText(),
                        u.path("email").asText(),
                        u.path("role").asText(),
                        u.path("verified").asBoolean() ? "Yes" : "No"));
                }
                Platform.runLater(() -> allUsersListView.setItems(items));
            } catch (Exception ex) {
                Platform.runLater(() -> verifyStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onApproveSeller(ActionEvent e) {
        String id = verifyUserIdField.getText();
        if (id.isBlank()) { verifyStatusLabel.setText("Enter a User ID."); return; }
        new Thread(() -> {
            try {
                adminRestService.approveSeller(Long.parseLong(id));
                Platform.runLater(() -> {
                    verifyStatusLabel.setStyle("-fx-text-fill: green;");
                    verifyStatusLabel.setText("User #" + id + " approved as Seller!");
                    onRefreshPendingSellers(null);
                    onRefreshUsers(null);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> verifyStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRejectSeller(ActionEvent e) {
        String id = verifyUserIdField.getText();
        if (id.isBlank()) { verifyStatusLabel.setText("Enter a User ID."); return; }
        new Thread(() -> {
            try {
                adminRestService.rejectSeller(Long.parseLong(id));
                Platform.runLater(() -> {
                    verifyStatusLabel.setStyle("-fx-text-fill: orange;");
                    verifyStatusLabel.setText("User #" + id + " rejected. Role reverted to Buyer.");
                    onRefreshPendingSellers(null);
                    onRefreshUsers(null);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> verifyStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onReviewDispute(ActionEvent e) {
        String id = disputeTxIdField.getText();
        if (id.isBlank()) { disputeStatusLabel.setText("Enter a Transaction ID."); return; }
        new Thread(() -> {
            try {
                adminRestService.reviewDispute(Long.parseLong(id));
                Platform.runLater(() -> {
                    disputeStatusLabel.setStyle("-fx-text-fill: orange;");
                    disputeStatusLabel.setText("TX#" + id + " is now under review.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> disputeStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onResolveBuyer(ActionEvent e) {
        String id = disputeTxIdField.getText();
        if (id.isBlank()) { disputeStatusLabel.setText("Enter a Transaction ID."); return; }
        new Thread(() -> {
            try {
                adminRestService.resolveDisputeBuyer(Long.parseLong(id));
                Platform.runLater(() -> {
                    disputeStatusLabel.setStyle("-fx-text-fill: blue;");
                    disputeStatusLabel.setText("Resolved in buyer's favour — refund issued.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> disputeStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onResolveSeller(ActionEvent e) {
        String id = disputeTxIdField.getText();
        if (id.isBlank()) { disputeStatusLabel.setText("Enter a Transaction ID."); return; }
        new Thread(() -> {
            try {
                adminRestService.resolveDisputeSeller(Long.parseLong(id));
                Platform.runLater(() -> {
                    disputeStatusLabel.setStyle("-fx-text-fill: green;");
                    disputeStatusLabel.setText("Resolved in seller's favour — funds released.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> disputeStatusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onLogout(ActionEvent e) {
        try { CampusApp.getInstance().loadLoginScreen(); } catch (Exception ex) { ex.printStackTrace(); }
    }
}