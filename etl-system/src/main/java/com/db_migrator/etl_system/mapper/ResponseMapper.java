package com.db_migrator.etl_system.mapper;

import com.db_migrator.etl_system.dto.response.OrganizationResponse;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.model.entity.user.Organization;
import com.db_migrator.etl_system.model.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResponseMapper {

    public OrganizationResponse toOrganizationResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .companyName(organization.getCompanyName())
                .location(organization.getLocation())
                .createdAt(organization.getCreatedAt().toString())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());

        OrganizationResponse organizationResponse = toOrganizationResponse(user.getOrganization());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organization(organizationResponse)
                .roles(roles)
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}
