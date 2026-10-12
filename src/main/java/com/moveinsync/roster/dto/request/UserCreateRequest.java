package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {
    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    private String role;

    public UserCreateRequest() {
    }

    public UserCreateRequest(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
