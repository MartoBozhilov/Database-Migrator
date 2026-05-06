package com.database_migrator.domain.auth.controller;

import com.database_migrator.domain.auth.dto.LoginRequest;
import com.database_migrator.domain.auth.dto.AuthResponse;
import com.database_migrator.domain.auth.dto.UserResponse;
import com.database_migrator.domain.common.util.SecurityUtils;
import com.database_migrator.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Long userId = securityUtils.getCurrentUser().getId();
        UserResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
}
