package com.campus.user.service;

import com.campus.user.config.JwtConfig;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements ITokenService {

    private final JwtConfig jwtConfig;

    public JwtTokenService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public String generateToken(String email, String role) {
        return jwtConfig.generateToken(email, role);
    }

    @Override
    public boolean isTokenValid(String token) {
        return jwtConfig.isTokenValid(token);
    }

    @Override
    public String getEmailFromToken(String token) {
        return jwtConfig.getEmailFromToken(token);
    }

    @Override
    public String getRoleFromToken(String token) {
        return jwtConfig.getRoleFromToken(token);
    }
}
