package com.smartprocure.config;

import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.repository.RoleRepository;
import com.smartprocure.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeds the default admin user at application startup.
 *
 * <p>Skipped in the {@code test} profile (integration tests supply their own data).
 * Idempotent: runs a {@code existsByEmail} check before inserting.
 *
 * <p>Override the defaults via environment variables:
 * <pre>
 *   ADMIN_EMAIL=ops@company.com
 *   ADMIN_PASSWORD=VeryStr0ng!Pass
 * </pre>
 *
 * <p><b>Production warning:</b> change the default admin password immediately
 * after first deployment. The default value is intentionally documented here
 * so operators know what to override.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@smartprocure.internal}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@SmartProcure2025!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Admin user already exists — skipping seed");
            return;
        }

        var adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found — ensure Flyway migration V7 ran successfully"));

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setDepartment("IT");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(new HashSet<>(Set.of(adminRole)));

        userRepository.save(admin);
        log.info("Default admin user seeded: {}", adminEmail);
    }
}
