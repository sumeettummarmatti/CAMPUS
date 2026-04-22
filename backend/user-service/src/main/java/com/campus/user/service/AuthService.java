package com.campus.user.service;

import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.UserDTO;
import com.campus.user.model.Role;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final IPasswordService passwordService;
    private final ITokenService tokenService;

    public AuthService(UserRepository userRepository, IPasswordService passwordService, ITokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    /**
     * Register a new user. Hashes password with BCrypt.
     */
    public UserDTO register(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + dto.getEmail());
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordService.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setHostelName(dto.getHostelName());
        user.setRole(dto.getRole() != null ? dto.getRole() : Role.BUYER);
        // Buyers are auto-verified. Only seller upgrade requests need admin approval.
        boolean needsApproval = (user.getRole() == Role.SELLER);
        user.setVerified(!needsApproval);
        user.setWalletBalance(new BigDecimal("1000.00"));
        user.setTotalSpent(BigDecimal.ZERO);
        user.setTotalEarned(BigDecimal.ZERO);
        ArrayList<String> modes = new ArrayList<>();
        modes.add("CAMPUS_WALLET");
        user.setEnabledPaymentModes(modes);

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    /**
     * Authenticate user and return JWT token.
     */
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return tokenService.generateToken(user.getEmail(), user.getRole().name());
    }

    // ── Mapping helper ──────────────────────────────────────

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setHostelName(user.getHostelName());
        dto.setRole(user.getRole());
        dto.setVerified(user.isVerified());
        dto.setWalletBalance(user.getWalletBalance());
        dto.setTotalSpent(user.getTotalSpent());
        dto.setTotalEarned(user.getTotalEarned());
        dto.setEnabledPaymentModes(user.getEnabledPaymentModes());
        return dto;
    }
}
