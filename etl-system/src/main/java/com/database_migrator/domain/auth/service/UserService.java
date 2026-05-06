package com.database_migrator.domain.auth.service;

import com.database_migrator.domain.auth.dto.UserCreateRequest;
import com.database_migrator.domain.auth.dto.UserResponse;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.auth.model.Organization;
import com.database_migrator.domain.auth.model.User;
import com.database_migrator.domain.auth.model.UserRole;
import com.database_migrator.domain.auth.model.UserRoleEnum;
import com.database_migrator.domain.auth.repository.OrganizationRepository;
import com.database_migrator.domain.auth.repository.UserRepository;
import com.database_migrator.domain.auth.repository.UserRoleRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import com.database_migrator.config.security.TokenVersionCache;
import com.database_migrator.domain.common.exception.ValidationException;
import com.database_migrator.domain.common.exception.BusinessRuleException;
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
            throw new ValidationException("Email already exists", List.of("Duplicate email: " + request.getEmail()));
        }

        Organization organization = resolveOrganization(request);
        User user = buildUser(request, organization);

        user = userRepository.save(user);

        return responseMapper.toUserResponse(user);
    }

    public UserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new BusinessRuleException("Access denied: User belongs to different organization",
                    "USER_ORGANIZATION_MISMATCH");
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
            throw new BusinessRuleException("Access denied: Only ADMIN can assign roles",
                    "INSUFFICIENT_PERMISSIONS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", roleEnum.name()));

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
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN can remove roles",
                    "INSUFFICIENT_PERMISSIONS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));


        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", roleEnum.name()));

        user.getRoles().remove(role);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);
        tokenVersionCache.invalidate(user.getEmail());

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse assignRoleToOrgUser(Long userId, UserRoleEnum roleEnum) {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && !SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN or MIGRATION_ADMIN can assign roles",
                    "INSUFFICIENT_PERMISSIONS");
        }

        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            if (roleEnum == UserRoleEnum.ADMIN || roleEnum == UserRoleEnum.MIGRATION_ADMIN) {
                throw new BusinessRuleException("Access denied: MIGRATION_ADMIN cannot assign ADMIN or MIGRATION_ADMIN roles",
                        "INSUFFICIENT_PERMISSIONS");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new BusinessRuleException("Access denied: User belongs to different organization",
                    "USER_ORGANIZATION_MISMATCH");
        }

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", roleEnum.name()));

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
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN) && !SecurityUtils.hasRole(UserRoleEnum.MIGRATION_ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN or MIGRATION_ADMIN can remove roles",
                    "INSUFFICIENT_PERMISSIONS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new BusinessRuleException("Access denied: User belongs to different organization",
                    "USER_ORGANIZATION_MISMATCH");
        }

        UserRole role = userRoleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", roleEnum.name()));

        user.getRoles().remove(role);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);
        tokenVersionCache.invalidate(user.getEmail());

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Long currentOrgId = securityUtils.getCurrentOrganizationId();
        if (!user.getOrganization().getId().equals(currentOrgId)) {
            throw new BusinessRuleException("Access denied: User belongs to different organization",
                    "USER_ORGANIZATION_MISMATCH");
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
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));
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
