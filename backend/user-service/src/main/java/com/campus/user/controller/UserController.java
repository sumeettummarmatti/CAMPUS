package com.campus.user.controller;

import com.campus.user.dto.TransactionSyncRequest;
import com.campus.user.dto.UserDTO;
import com.campus.user.dto.WalletModesRequest;
import com.campus.user.service.SellerVerificationService;
import com.campus.user.service.UserService;
import com.campus.user.service.UserTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user profile and seller verification (MVC: Controller).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SellerVerificationService sellerVerificationService;
    private final UserTransactionService userTransactionService;

    public UserController(UserService userService,
                          SellerVerificationService sellerVerificationService,
                          UserTransactionService userTransactionService) {
        this.userService = userService;
        this.sellerVerificationService = sellerVerificationService;
        this.userTransactionService = userTransactionService;
    }

    /**
     * GET /api/users/{id} — retrieve user profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * GET /api/users — list all users (admin).
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> listAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * GET /api/users/me — returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile(
            org.springframework.security.core.Authentication auth) {
        String email = auth.getName(); // set by JwtAuthFilter
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    /**
     * PUT /api/users/{id} — update user profile.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }

    @GetMapping("/{id}/wallet/modes")
    public ResponseEntity<List<String>> walletModes(@PathVariable Long id) {
        return ResponseEntity.ok(userTransactionService.getEnabledModes(id));
    }

    @PutMapping("/{id}/wallet/modes")
    public ResponseEntity<List<String>> updateWalletModes(@PathVariable Long id, @RequestBody WalletModesRequest request) {
        return ResponseEntity.ok(userTransactionService.updateEnabledModes(id, request.getModes()));
    }

    @GetMapping("/{id}/transactions/buyer")
    public ResponseEntity<?> buyerTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(userTransactionService.buyerTransactions(id));
    }

    @GetMapping("/{id}/transactions/seller")
    public ResponseEntity<?> sellerTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(userTransactionService.sellerTransactions(id));
    }

    @GetMapping("/internal/{id}/wallet/modes")
    public ResponseEntity<List<String>> internalWalletModes(@PathVariable Long id) {
        return ResponseEntity.ok(userTransactionService.getEnabledModes(id));
    }

    @PostMapping("/internal/transactions/sync")
    public ResponseEntity<Void> syncTransaction(@RequestBody TransactionSyncRequest request) {
        userTransactionService.syncTransaction(request);
        return ResponseEntity.ok().build();
    }

    // ── Seller Verification ─────────────────────────────

    /**
     * POST /api/users/{id}/verify/request — buyer requests seller upgrade.
     */
    @PostMapping("/{id}/verify/request")
    public ResponseEntity<Map<String, String>> requestVerification(@PathVariable Long id) {
        sellerVerificationService.requestVerification(id);
        return ResponseEntity.ok(Map.of("message", "Verification request submitted"));
    }

    /**
     * POST /api/users/{id}/verify/approve — admin approves seller.
     */
    @PostMapping("/{id}/verify/approve")
    public ResponseEntity<Map<String, String>> approveSeller(@PathVariable Long id) {
        sellerVerificationService.approveSeller(id);
        return ResponseEntity.ok(Map.of("message", "Seller approved"));
    }

    /**
     * POST /api/users/{id}/verify/reject — admin rejects seller.
     */
    @PostMapping("/{id}/verify/reject")
    public ResponseEntity<Map<String, String>> rejectSeller(@PathVariable Long id) {
        sellerVerificationService.rejectSeller(id);
        return ResponseEntity.ok(Map.of("message", "Seller rejected"));
    }
}
