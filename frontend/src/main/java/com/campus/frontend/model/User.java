package com.campus.frontend.model;

/**
 * Model representing the authenticated user.
 */
public class User {
    private Long id;
    private String fullName;
    private String email;
    // role will be "USER" for everyone, "ADMIN" for admins.
    // It does NOT determine buying/selling capability — all users can do both.
    private String role;
    private boolean verified;

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
}
