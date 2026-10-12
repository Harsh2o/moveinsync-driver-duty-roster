package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.request.UserCreateRequest;
import com.moveinsync.roster.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Administration of system users and roles")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create system user", description = "Admin only. Creates administrative, approver, or employee accounts.")
    public ResponseEntity<Map<String, String>> createUser(@Valid @RequestBody UserCreateRequest request) {
        authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User created successfully",
                "username", request.getUsername()
        ));
    }
}
