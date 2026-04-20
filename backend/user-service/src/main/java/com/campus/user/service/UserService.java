package com.campus.user.service;

import com.campus.user.dto.UserDTO;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business-logic layer for user management (MVC: Service).
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
