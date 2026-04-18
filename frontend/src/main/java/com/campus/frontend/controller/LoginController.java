package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
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
}
