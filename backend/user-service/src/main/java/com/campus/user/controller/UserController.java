package com.campus.user.controller;

import com.campus.user.dto.UserDTO;
import com.campus.user.service.SellerVerificationService;
import com.campus.user.service.UserService;
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

    public UserController(UserService userService) {
        this.userService = userService;
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
}
