package com.campus.user.service;

import com.campus.user.model.Role;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Handles seller verification workflow.
 * Transitions: BUYER → (request) → PENDING → (approve/reject) → SELLER / BUYER
 */
@Service
public class SellerVerificationService {
    private final UserRepository userRepository;

    public SellerVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Buyer requests upgrade to seller role.
     * We mark verified=false but keep role=BUYER until admin approves.
     * The admin sees users with role=SELLER and verified=false as "pending".
     */
    public void requestVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == Role.SELLER && user.isVerified()) {
            throw new IllegalStateException("User is already a verified seller");
        }

        // Set role to SELLER but unverified — admin must approve
        user.setRole(Role.SELLER);
        user.setVerified(false);
        userRepository.save(user);
        // NOTE: user cannot create auctions until verified=true (the AuctionService
        // should check this — see Note below)
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
