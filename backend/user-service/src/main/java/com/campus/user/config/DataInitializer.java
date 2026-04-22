package com.campus.user.config;

import com.campus.user.model.Role;
import com.campus.user.model.User;
import com.campus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Runs once on startup. Creates the default admin account if it doesn't exist.
 * Admin credentials: admin@campus.edu / admin123
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@campus.edu")) {
            // Create fresh admin
            User admin = new User();
            admin.setEmail("admin@campus.edu");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFullName("Platform Admin");
            admin.setHostelName("Admin Block");
            admin.setRole(Role.ADMIN);
            admin.setVerified(true);
            admin.setWalletBalance(BigDecimal.ZERO);
            admin.setTotalSpent(BigDecimal.ZERO);
            admin.setTotalEarned(BigDecimal.ZERO);
            admin.setTotalDeposited(BigDecimal.ZERO);
            ArrayList<String> modes = new ArrayList<>();
            modes.add("CAMPUS_WALLET");
            admin.setEnabledPaymentModes(modes);
            userRepository.save(admin);
            log.info("✅ Default admin account created: admin@campus.edu / admin123");
        } else {
            // Admin exists — but force-reset the password hash in case it's corrupted
            User admin = userRepository.findByEmail("admin@campus.edu").get();
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setVerified(true);
            userRepository.save(admin);
            log.info("✅ Admin password hash refreshed on startup");
        }
    }
}