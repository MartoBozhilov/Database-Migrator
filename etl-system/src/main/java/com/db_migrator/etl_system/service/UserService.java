package com.db_migrator.etl_system.service;

import com.db_migrator.etl_system.dto.request.UserCreateRequest;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.mapper.ResponseMapper;
import com.db_migrator.etl_system.model.entity.user.Organization;
import com.db_migrator.etl_system.model.entity.user.User;
import com.db_migrator.etl_system.model.entity.user.UserRole;
import com.db_migrator.etl_system.model.enums.UserRoleEnum;
import com.db_migrator.etl_system.repository.OrganizationRepository;
import com.db_migrator.etl_system.repository.UserRepository;
import com.db_migrator.etl_system.repository.UserRoleRepository;
import com.db_migrator.etl_system.security.SecurityUtils;
import com.db_migrator.etl_system.security.TokenVersionCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;
    private final TokenVersionCache tokenVersionCache;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Organization organization = resolveOrganization(request);
        User user = buildUser(request, organization);

        user = userRepository.save(user);

        return responseMapper.toUserResponse(user);
    }

    public UserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new RuntimeException("Access denied: User belongs to different organization");
        }

        return responseMapper.toUserResponse(user);
    }

    public List<UserResponse> findAllInOrganization() {
        Long organizationId = securityUtils.getCurrentOrganizationId();
        List<User> users = userRepository.findByOrganization_Id(organizationId);

        return users.stream()
                .map(responseMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse assignRole(Long userId, UserRoleEnum roleEnum) {
        // Only ADMIN can assign roles (accessed via /api/admin/users/{id}/roles)
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN can assign roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ADMIN can assign roles to users in any organization (no org check)

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            user.setTokenVersion(user.getTokenVersion() + 1);
            user = userRepository.save(user);
            tokenVersionCache.invalidate(user.getEmail());
        }

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse removeRole(Long userId, UserRoleEnum roleEnum) {
        // Only ADMIN can remove roles (accessed via /api/admin/users/{id}/roles)
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN can remove roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ADMIN can remove roles from users in any organization (no org check)

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.getRoles().remove(role);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);
        tokenVersionCache.invalidate(user.getEmail());

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse assignRoleToOrgUser(Long userId, UserRoleEnum roleEnum) {
        // ADMIN or MIGRATION_ADMIN can assign roles (accessed via /api/users/{id}/roles)
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && !SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN or MIGRATION_ADMIN can assign roles");
        }

        // MIGRATION_ADMIN can only assign operational roles
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            if (roleEnum == UserRoleEnum.ADMIN || roleEnum == UserRoleEnum.MIGRATION_ADMIN) {
                throw new RuntimeException("Access denied: MIGRATION_ADMIN cannot assign ADMIN or MIGRATION_ADMIN roles");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Enforce organization isolation (both ADMIN and MIGRATION_ADMIN)
        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new RuntimeException("Access denied: User belongs to different organization");
        }

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            user.setTokenVersion(user.getTokenVersion() + 1);
            user = userRepository.save(user);
            tokenVersionCache.invalidate(user.getEmail());
        }

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse removeRoleFromOrgUser(Long userId, UserRoleEnum roleEnum) {
        // ADMIN or MIGRATION_ADMIN can remove roles (accessed via /api/users/{id}/roles)
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && !SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN or MIGRATION_ADMIN can remove roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Enforce organization isolation (both ADMIN and MIGRATION_ADMIN)
        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new RuntimeException("Access denied: User belongs to different organization");
        }

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.getRoles().remove(role);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);
        tokenVersionCache.invalidate(user.getEmail());

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new RuntimeException("Access denied: User belongs to different organization");
        }

        userRepository.delete(user);
    }

    private Organization resolveOrganization(UserCreateRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // ADMIN can specify organizationId to create users in any organization
        // MIGRATION_ADMIN can only create users in their own organization
        if (SecurityUtils.hasRole(UserRoleEnum.ADMIN) && request.getOrganizationId() != null) {
            return currentUser.getOrganization().getId().equals(request.getOrganizationId())
                    ? currentUser.getOrganization()
                    : organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found with id: " + request.getOrganizationId()));
        } else {
            return currentUser.getOrganization();
        }
    }

    private User buildUser(UserCreateRequest request, Organization organization) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .organization(organization)
                .roles(new ArrayList<>())
                .createdAt(new Date())
                .build();
    }
}
