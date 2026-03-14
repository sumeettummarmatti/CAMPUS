package com.campus.user.service;

import com.campus.user.model.Role;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Handles seller verification workflow.
 * Transitions: BUYER → (request) → PENDING → (approve/reject) → SELLER / BUYER
 */
@Service
@RequiredArgsConstructor
public class SellerVerificationService {

    private final UserRepository userRepository;

    /**
     * Buyer requests upgrade to seller role.
     * Sets verified = false until admin approves.
     */
    public void requestVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == Role.SELLER && user.isVerified()) {
            throw new IllegalStateException("User is already a verified seller");
        }

        user.setRole(Role.SELLER);
        user.setVerified(false);
        userRepository.save(user);
    }

    /**
     * Admin approves seller verification.
     */
    public void approveSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() != Role.SELLER) {
            throw new IllegalStateException("User has not requested seller verification");
        }

        user.setVerified(true);
        userRepository.save(user);
    }

    /**
     * Admin rejects seller verification — reverts to BUYER.
     */
    public void rejectSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setRole(Role.BUYER);
        user.setVerified(false);
        userRepository.save(user);
    }
}
