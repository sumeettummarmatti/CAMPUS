package com.campus.user.service;

public interface ITokenService {
    String generateToken(String email, String role);
    boolean isTokenValid(String token);
    String getEmailFromToken(String token);
    String getRoleFromToken(String token);
}
