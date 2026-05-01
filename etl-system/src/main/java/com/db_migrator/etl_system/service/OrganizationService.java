package com.db_migrator.etl_system.service;

import com.db_migrator.etl_system.dto.request.CreateOrganizationUserRequest;
import com.db_migrator.etl_system.dto.request.OrganizationCreateRequest;
import com.db_migrator.etl_system.dto.response.OrganizationResponse;
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
        // Only ADMIN can create organizations
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN can create organizations");
        }

        // Check if organization name already exists
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name '" + request.getName() + "' already exists");
        }

        Organization organization = buildOrganization(request);

        organization = organizationRepository.save(organization);

        return responseMapper.toOrganizationResponse(organization);
    }

    @Transactional
    public UserResponse createMigrationAdmin(CreateOrganizationUserRequest request) {
        // Only ADMIN can create MIGRATION_ADMIN for an organization
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN can create organization users");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        User user = buildUser(request, organization);

        assignAdministrativeRoles(user, UserRoleEnum.MIGRATION_ADMIN);

        user = userRepository.save(user);

        return responseMapper.toUserResponse(user);
    }

    public OrganizationResponse findById(Long orgId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            Long currentOrgId = securityUtils.getCurrentOrganizationId();
            if (!organization.getId().equals(currentOrgId)) {
                throw new RuntimeException("Access denied: Cannot view other organizations");
            }
        }

        return responseMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse findByName(String name) {
        Organization organization = organizationRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Organization not found with name: " + name));

        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            Long currentOrgId = securityUtils.getCurrentOrganizationId();
            if (!organization.getId().equals(currentOrgId)) {
                throw new RuntimeException("Access denied: Cannot view other organizations");
            }
        }

        return responseMapper.toOrganizationResponse(organization);
    }

    public List<OrganizationResponse> findAll() {
        if (!SecurityUtils.hasRole(UserRoleEnum.ADMIN)) {
            throw new RuntimeException("Access denied: Only ADMIN can list all organizations");
        }

        return organizationRepository.findAll().stream()
                .map(responseMapper::toOrganizationResponse)
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
                .orElseThrow(() -> new RuntimeException(adminRole + " role not found"));
        user.getRoles().add(administrativeRole);

        // Add all operational roles
        UserRole connectorUserRole = userRoleRepository.findByRole(UserRoleEnum.CONNECTOR_USER)
                .orElseThrow(() -> new RuntimeException("CONNECTOR_USER role not found"));
        user.getRoles().add(connectorUserRole);

        UserRole transformationUserRole = userRoleRepository.findByRole(UserRoleEnum.TRANSFORMATION_MODEL_USER)
                .orElseThrow(() -> new RuntimeException("TRANSFORMATION_MODEL_USER role not found"));
        user.getRoles().add(transformationUserRole);

        UserRole cycleExecutionUserRole = userRoleRepository.findByRole(UserRoleEnum.CYCLE_EXECUTION_USER)
                .orElseThrow(() -> new RuntimeException("CYCLE_EXECUTION_USER role not found"));
        user.getRoles().add(cycleExecutionUserRole);
    }
}
