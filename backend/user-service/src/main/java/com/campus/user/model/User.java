package com.campus.user.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * JPA entity representing a platform user (buyer, seller, or admin).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    private String hostelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal walletBalance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarned;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_wallet_modes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "payment_mode", nullable = false)
    private List<String> enabledPaymentModes = new ArrayList<>();

    public User() {}

    public User(Long id, String email, String passwordHash, String fullName, String hostelName, Role role, boolean verified, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.hostelName = hostelName;
        this.role = role;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = Role.BUYER;
        }
        if (this.walletBalance == null) {
            this.walletBalance = new BigDecimal("1000.00");
        }
        if (this.totalSpent == null) {
            this.totalSpent = BigDecimal.ZERO;
        }
        if (this.totalEarned == null) {
            this.totalEarned = BigDecimal.ZERO;
        }
        if (this.enabledPaymentModes == null) {
            this.enabledPaymentModes = new ArrayList<>();
        }
        if (this.enabledPaymentModes.isEmpty()) {
            this.enabledPaymentModes.add("CAMPUS_WALLET");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getTotalEarned() { return totalEarned; }
    public void setTotalEarned(BigDecimal totalEarned) { this.totalEarned = totalEarned; }

    public List<String> getEnabledPaymentModes() { return enabledPaymentModes; }
    public void setEnabledPaymentModes(List<String> enabledPaymentModes) { this.enabledPaymentModes = enabledPaymentModes; }
}
