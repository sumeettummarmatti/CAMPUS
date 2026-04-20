package com.campus.frontend.controller;

import com.campus.frontend.CampusApp;
import com.campus.frontend.service.PaymentRestService;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PaymentOptionsController {

    @FXML private Label auctionDetailsLabel;
    @FXML private RadioButton upiRadio;
    @FXML private RadioButton walletRadio;
    @FXML private RadioButton cashRadio;
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton netBankRadio;
    @FXML private Label statusLabel;
    @FXML private Button payBtn;

    private PaymentRestService paymentRestService;
    private Long auctionId;
    private Long sellerId;
    private Long winnerId;
    private double amount;
    private String auctionTitle;
    private Runnable onSuccess;

    public void initData(Long auctionId, Long sellerId, Long winnerId, double amount, String title, Runnable onSuccess) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.auctionTitle = title;
        this.onSuccess = onSuccess;

        String token = CampusApp.getInstance().getRestService().getAuthToken();
        this.paymentRestService = new PaymentRestService(token);
        applyModeAccess();

        auctionDetailsLabel.setText(String.format("%s (Auction #%d) — ₹%.2f", title, auctionId, amount));
    }

    @FXML
    public void onPay(ActionEvent e) {
        String method = getSelectedMethod();
        statusLabel.setText("Initiating payment via " + method + "...");
        payBtn.setDisable(true);

        new Thread(() -> {
            try {
                // 1. Initiate
                JsonNode tx = paymentRestService.initiatePayment(auctionId, winnerId, sellerId, amount, method);
                Long txId = tx.path("id").asLong();

                Platform.runLater(() -> statusLabel.setText("Confirming transaction #" + txId + "..."));

                // 2. Confirm and Hold in Escrow
                paymentRestService.payWithMethod(txId, method);

                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Payment successful! Funds are now in Escrow.");
                    if (onSuccess != null) onSuccess.run();
                    
                    // Close after 2 seconds
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
                        Platform.runLater(this::close);
                    }).start();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Payment failed: " + ex.getMessage());
                    payBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    public void onCancel(ActionEvent e) {
        close();
    }

    private String getSelectedMethod() {
        if (upiRadio.isSelected()) return "GOOGLE_PAY";
        if (cashRadio.isSelected()) return "CASH";
        if (cardRadio.isSelected()) return "CARD";
        if (netBankRadio.isSelected()) return "NET_BANKING";
        return "CAMPUS_WALLET";
    }

    private void close() {
        ((Stage) auctionDetailsLabel.getScene().getWindow()).close();
    }

    private void applyModeAccess() {
        java.util.List<String> enabled = CampusApp.getInstance().getCurrentUser().getEnabledPaymentModes();
        if (enabled == null || enabled.isEmpty()) {
            walletRadio.setSelected(true);
            return;
        }
        walletRadio.setDisable(!enabled.contains("CAMPUS_WALLET"));
        upiRadio.setDisable(!(enabled.contains("GOOGLE_PAY") || enabled.contains("UPI")));
        cardRadio.setDisable(!(enabled.contains("CARD") || enabled.contains("CREDIT_CARD")));
        cashRadio.setDisable(!(enabled.contains("CASH") || enabled.contains("CASH_ON_DELIVERY")));
        netBankRadio.setDisable(!enabled.contains("NET_BANKING"));

        if (walletRadio.isDisable() && walletRadio.isSelected()) {
            if (!upiRadio.isDisable()) upiRadio.setSelected(true);
            else if (!cardRadio.isDisable()) cardRadio.setSelected(true);
            else if (!cashRadio.isDisable()) cashRadio.setSelected(true);
            else if (!netBankRadio.isDisable()) netBankRadio.setSelected(true);
        }
    }
}
