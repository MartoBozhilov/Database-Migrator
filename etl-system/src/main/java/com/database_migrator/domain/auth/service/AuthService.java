package com.database_migrator.domain.auth.service;

import com.database_migrator.domain.auth.dto.ChangePasswordRequest;
import com.database_migrator.domain.auth.dto.LoginRequest;
import com.database_migrator.domain.auth.dto.AuthResponse;
import com.database_migrator.domain.auth.dto.UserResponse;
import com.database_migrator.domain.common.exception.ValidationException;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.auth.model.User;
import com.database_migrator.domain.auth.repository.UserRepository;
import com.database_migrator.config.security.JwtTokenProvider;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ResponseMapper responseMapper;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return responseMapper.toUserResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            errors.add("Current password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            errors.add("New password is required");
        }
        if (request.getConfirmPassword() == null || request.getConfirmPassword().isEmpty()) {
            errors.add("Confirm password is required");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Password change validation failed", errors);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            errors.add("New password and confirm password do not match");
            throw new ValidationException("Password change validation failed", errors);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (request.getNewPassword().length() < 6) {
            errors.add("New password must be at least 6 characters long");
            throw new ValidationException("Password change validation failed", errors);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);
        List<String> roles = extractRoleNames(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .organizationId(user.getOrganization().getId())
                .organizationName(user.getOrganization().getName())
                .roles(roles)
                .expiresIn(jwtExpirationMs)
                .build();
    }

    private List<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toList());
    }
}
