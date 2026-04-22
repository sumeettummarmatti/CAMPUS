package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PendingApprovalController {

    @FXML private Label emailLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        User user = CampusApp.getInstance().getCurrentUser();
        if (user != null) {
            emailLabel.setText("Logged in as: " + user.getEmail());
        }
    }

    @FXML
    public void onCheckAgain(ActionEvent e) {
        statusLabel.setText("Checking your approval status...");
        new Thread(() -> {
            try {
                // Re-fetch the user profile to see if verified changed
                User latest = CampusApp.getInstance()
                        .getRestService().fetchCurrentUserProfile();

                if (latest.isVerified()) {
                    // Approved! Update the stored user and go to dashboard
                    CampusApp.getInstance().setCurrentUser(latest);
                    Platform.runLater(() -> {
                        try {
                            CampusApp.getInstance().loadDashboard();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } else {
                    Platform.runLater(() ->
                        statusLabel.setText(
                            "Not approved yet. Please check back later."));
                }
            } catch (Exception ex) {
                Platform.runLater(() ->
                    statusLabel.setText("Error checking status: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onLogout(ActionEvent e) {
        try {
            CampusApp.getInstance().loadLoginScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}