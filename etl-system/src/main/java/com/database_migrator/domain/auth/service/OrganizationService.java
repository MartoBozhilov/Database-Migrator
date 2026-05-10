package com.database_migrator.domain.auth.service;

import com.database_migrator.domain.auth.dto.CreateOrganizationUserRequest;
import com.database_migrator.domain.auth.dto.OrganizationCreateRequest;
import com.database_migrator.domain.auth.dto.OrganizationResponse;
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
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;

    @Transactional
    public OrganizationResponse create(OrganizationCreateRequest request) {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN can create organizations",
                    "INSUFFICIENT_PERMISSIONS");
        }

        if (organizationRepository.existsByName(request.getName())) {
            throw new ValidationException("Organization with name '" + request.getName() + "' already exists",
                    List.of("Duplicate organization name: " + request.getName()));
        }

        Organization organization = buildOrganization(request);

        organization = organizationRepository.save(organization);

        return responseMapper.toOrganizationResponse(organization);
    }

    @Transactional
    public UserResponse createMigrationAdmin(CreateOrganizationUserRequest request) {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN can create MIGRATION_ADMIN to Organisation",
                    "INSUFFICIENT_PERMISSIONS");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists", List.of("Duplicate email: " + request.getEmail()));
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));

        User user = buildUser(request, organization);

        assignAdministrativeRoles(user, UserRoleEnum.MIGRATION_ADMIN);

        user = userRepository.save(user);

        return responseMapper.toUserResponse(user);
    }

    public OrganizationResponse findById(Long orgId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            Long currentOrgId = securityUtils.getCurrentOrganizationId();
            if (!organization.getId().equals(currentOrgId)) {
                throw new BusinessRuleException("Access denied: Cannot view other organizations",
                        "ORGANIZATION_ACCESS_DENIED");
            }
        }

        return responseMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse findByName(String name) {
        Organization organization = organizationRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", name));

        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            Long currentOrgId = securityUtils.getCurrentOrganizationId();
            if (!organization.getId().equals(currentOrgId)) {
                throw new BusinessRuleException("Access denied: Cannot view other organizations",
                        "ORGANIZATION_ACCESS_DENIED");
            }
        }

        return responseMapper.toOrganizationResponse(organization);
    }

    public List<OrganizationResponse> findAll() {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN can list all organizations",
                    "INSUFFICIENT_PERMISSIONS");
        }

        return organizationRepository.findAll().stream()
                .map(responseMapper::toOrganizationResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByOrganization(Long orgId) {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new BusinessRuleException("Access denied: Only ADMIN can view organization users",
                    "INSUFFICIENT_PERMISSIONS");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        List<User> users = userRepository.findByOrganization_Id(orgId);
        return users.stream()
                .map(responseMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    private Organization buildOrganization(OrganizationCreateRequest request) {
        return Organization.builder()
                .name(request.getName())
                .companyName(request.getCompanyName())
                .location(request.getLocation())
                .createdAt(new Date())
                .build();
    }

    private User buildUser(CreateOrganizationUserRequest request, Organization organization) {
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

    public void assignAdministrativeRoles(User user, UserRoleEnum adminRole) {
        // Assign administrative role (ADMIN or MIGRATION_ADMIN)
        UserRole administrativeRole = userRoleRepository.findByRole(adminRole)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", adminRole.name()));
        user.getRoles().add(administrativeRole);

        // Add all operational roles
        UserRole connectorUserRole = userRoleRepository.findByRole(UserRoleEnum.CONNECTOR_USER)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", "CONNECTOR_USER"));
        user.getRoles().add(connectorUserRole);

        UserRole transformationUserRole = userRoleRepository.findByRole(UserRoleEnum.TRANSFORMATION_MODEL_USER)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", "TRANSFORMATION_MODEL_USER"));
        user.getRoles().add(transformationUserRole);

        UserRole cycleExecutionUserRole = userRoleRepository.findByRole(UserRoleEnum.CYCLE_EXECUTION_USER)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", "CYCLE_EXECUTION_USER"));
        user.getRoles().add(cycleExecutionUserRole);
    }
}
