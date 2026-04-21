package com.campus.frontend.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing the authenticated user.
 */
public class User {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private boolean verified;
    private double walletBalance;
    private double totalSpent;
    private double totalEarned;
    private double totalDeposited;
    private List<String> enabledPaymentModes = new ArrayList<>();

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public double getTotalEarned() { return totalEarned; }
    public void setTotalEarned(double totalEarned) { this.totalEarned = totalEarned; }

    public double getTotalDeposited() { return totalDeposited; }
    public void setTotalDeposited(double totalDeposited) { this.totalDeposited = totalDeposited; }

    public List<String> getEnabledPaymentModes() { return enabledPaymentModes; }
    public void setEnabledPaymentModes(List<String> enabledPaymentModes) { this.enabledPaymentModes = enabledPaymentModes; }
}
