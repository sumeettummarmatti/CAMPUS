package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the main dashboard post-login.
 */
public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        // Will be populated with actual user data later via GET /api/users/me
        welcomeLabel.setText("Welcome to CAMPUS! You are securely logged in.");
    }

    @FXML
    public void onLogout(ActionEvent event) {
        try {
            // Drop token and go to login
            CampusApp.getInstance().loadLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
