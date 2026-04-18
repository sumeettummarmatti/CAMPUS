package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the Registration screen.
 */
public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML private Label errorLabel;


    @FXML
    public void onRegister(ActionEvent event) {
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();


        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            errorLabel.setText("All fields are required.");
            return;
        }

        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }

        errorLabel.setText("Registering...");

        new Thread(() -> {
            try {
                CampusApp.getInstance().getRestService().register(fullName, email, password);
                Platform.runLater(() -> {
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText("Registration successful! Please login.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText(e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void onGoToLogin(ActionEvent event) {
        try {
            CampusApp.getInstance().loadLoginScreen();
        } catch (Exception e) {
            errorLabel.setText("Error loading login screen.");
            e.printStackTrace();
        }
    }
}
