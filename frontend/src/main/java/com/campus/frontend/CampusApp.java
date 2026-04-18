package com.campus.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.campus.frontend.service.UserRestService;
import com.campus.frontend.model.User;

/**
 * Main application class for the CAMPUS JavaFX frontend.
 */
public class CampusApp extends Application {

    private static CampusApp instance;
    private Stage primaryStage;
    
    // Global shared REST client holding the auth token
    private UserRestService restService;

    private User currentUser;

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.primaryStage = primaryStage;
        this.restService = new UserRestService();
        
        primaryStage.setTitle("CAMPUS Platform");
        loadLoginScreen();
        primaryStage.show();
    }

    public static CampusApp getInstance() {
        return instance;
    }

    public UserRestService getRestService() {
        return restService;
    }

    /**
     * Transition to Login Screen.
     */
    public void loadLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 400, 300));
    }

    /**
     * Transition to Register Screen.
     */
    public void loadRegisterScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 400, 400));
    }

    /**
     * Transition to main application Dashboard.
     */
    public void loadDashboard() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 800, 600));
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Transition to Profile page */
    public void loadProfileScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 800, 600));
    }
}
