package com.campus.user.service;

import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.UserDTO;
import com.campus.user.model.Role;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import com.campus.user.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business-logic layer for user management (MVC: Service).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    /**
     * Register a new user. Hashes password with BCrypt.
     */
    public UserDTO register(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + dto.getEmail());
        }

        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .hostelName(dto.getHostelName())
                .role(dto.getRole() != null ? dto.getRole() : Role.BUYER)
                .verified(false)
                .build();

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    /**
     * Authenticate user and return JWT token.
     */
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return jwtConfig.generateToken(user.getEmail(), user.getRole().name());
    }

    /**
     * Find user by ID.
     */
    public UserDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toDTO(user);
    }

    /**
     * Find user by email.
     */
    public UserDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return toDTO(user);
    }

    /**
     * Retrieve all users (admin use).
     */
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Update user profile.
     */
    public UserDTO updateProfile(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getHostelName() != null) user.setHostelName(dto.getHostelName());

        User updated = userRepository.save(user);
        return toDTO(updated);
    }

    // ── Mapping helper ──────────────────────────────────────

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .hostelName(user.getHostelName())
                .role(user.getRole())
                .verified(user.isVerified())
                .build();
    }
}
