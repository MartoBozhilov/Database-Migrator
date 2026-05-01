package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.LoginRequest;
import com.db_migrator.etl_system.dto.response.AuthResponse;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.security.SecurityUtils;
import com.db_migrator.etl_system.service.AuthService;
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
