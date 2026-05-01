package com.db_migrator.etl_system.config;

import com.db_migrator.etl_system.model.entity.user.Organization;
import com.db_migrator.etl_system.model.entity.user.User;
import com.db_migrator.etl_system.model.entity.user.UserRole;
import com.db_migrator.etl_system.model.enums.UserRoleEnum;
import com.db_migrator.etl_system.repository.OrganizationRepository;
import com.db_migrator.etl_system.repository.UserRepository;
import com.db_migrator.etl_system.repository.UserRoleRepository;
import com.db_migrator.etl_system.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationService organizationService;

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeDefaultAdminUser();
    }

    private void initializeRoles() {
        log.info("Initializing user roles...");

        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if (userRoleRepository.findByRole(roleEnum).isEmpty()) {
                UserRole role = new UserRole();
                role.setRole(roleEnum);
                userRoleRepository.save(role);
                log.info("Created role: {}", roleEnum);
            }
        }

        log.info("User roles initialization complete. Total roles: {}", UserRoleEnum.values().length);
    }

    private void initializeDefaultAdminUser() {
        // Only create admin user if no users exist
        if (userRepository.count() > 0) {
            log.info("Users already exist in database. Skipping default admin user creation.");
            return;
        }

        log.info("Creating default admin user...");

        // Create default organization
        Organization organization = Organization.builder()
                .name("System Organization")
                .companyName("System")
                .location("Default")
                .createdAt(new Date())
                .build();
        organization = organizationRepository.save(organization);
        log.info("Created default organization: {}", organization.getName());

        // Create admin user
        User adminUser = User.builder()
                .username("admin")
                .email("admin@system.com")
                .password(passwordEncoder.encode("Admin123!"))
                .firstName("System")
                .lastName("Administrator")
                .organization(organization)
                .roles(new ArrayList<>())
                .createdAt(new Date())
                .build();

        // Assign ADMIN role and all operational roles
        organizationService.assignAdministrativeRoles(adminUser, UserRoleEnum.ADMIN);

        adminUser = userRepository.save(adminUser);

        log.info("========================================");
        log.info("Default admin user created successfully:");
        log.info("  Email: admin@system.com");
        log.info("  Password: Admin123!");
        log.info("  Roles: {}", adminUser.getRoles().stream()
                .map(role -> role.getRole().name())
                .toList());
        log.info("========================================");
    }
}
