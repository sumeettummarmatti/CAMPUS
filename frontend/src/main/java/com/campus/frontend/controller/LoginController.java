package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the Login screen.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void onLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        errorLabel.setText("Logging in...");

        new Thread(() -> {
            try {
                User loggedInUser = CampusApp.getInstance().getRestService().login(email, password);
                Platform.runLater(() -> {
                    try {
                        CampusApp.getInstance().setCurrentUser(loggedInUser);
                        CampusApp.getInstance().loadDashboard();
                    } catch (Exception e) {
                        errorLabel.setText("Error loading dashboard.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText(e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onGoToRegister(ActionEvent event) {
        try {
            CampusApp.getInstance().loadRegisterScreen();
        } catch (Exception e) {
            errorLabel.setText("Error loading register screen.");
            e.printStackTrace();
        }
    }

    @FXML
    public void onDemoSetup(ActionEvent event) {
        errorLabel.setText("Setting up demo data (this takes a few seconds)...");
        new Thread(() -> {
            try {
                com.campus.frontend.service.UserRestService userService = CampusApp.getInstance().getRestService();
                
                // 1. Create demo users (ignore errors if they already exist)
                try { userService.register("Demo Seller", "seller@demo.com", "seller123", "SELLER"); } catch (Exception ignore) {}
                try { userService.register("Demo Buyer", "buyer@demo.com", "buyer123", "BUYER"); } catch (Exception ignore) {}

                // 2. Login as the seller to create some auctions
                User sellerUser = userService.login("seller@demo.com", "seller123");
                com.campus.frontend.service.AuctionRestService auctionService = new com.campus.frontend.service.AuctionRestService(userService.getAuthToken());

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

                // Create a couple of mock auctions (ignore errors if we run it multiple times and it doesn't matter)
                try {
                    com.fasterxml.jackson.databind.JsonNode a1 = auctionService.createAuction(
                        "Vintage Bicycle", "A retro 1980s bicycle in perfect working condition.",
                        1200.0, 900.0, 
                        now.plusMinutes(1).format(formatter) + ":00", 
                        now.plusDays(2).format(formatter) + ":00", 
                        sellerUser.getId()
                    );
                    auctionService.scheduleAuction(a1.path("id").asLong());
                } catch (Exception ignore) {}

                try {
                    com.fasterxml.jackson.databind.JsonNode a2 = auctionService.createAuction(
                        "Used MacBook Pro M1", "Barely used, 16GB RAM, 512GB SSD.",
                        80000.0, 75000.0, 
                        now.plusMinutes(2).format(formatter) + ":00", 
                        now.plusDays(5).format(formatter) + ":00", 
                        sellerUser.getId()
                    );
                    auctionService.scheduleAuction(a2.path("id").asLong());
                } catch (Exception ignore) {}

                // 3. Pre-fill the login form with buyer credentials so the user can just click Login
                Platform.runLater(() -> {
                    emailField.setText("buyer@demo.com");
                    passwordField.setText("buyer123");
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText("Setup complete! Buyer credentials filled. Demo Seller is 'seller@demo.com' / 'seller123'.");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Demo setup failed: " + e.getMessage());
                });
            }
        }).start();
    }
}
