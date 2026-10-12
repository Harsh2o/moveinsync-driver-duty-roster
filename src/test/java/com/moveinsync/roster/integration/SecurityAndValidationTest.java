package com.moveinsync.roster.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.roster.dto.request.DriverRequest;
import com.moveinsync.roster.dto.request.LoginRequest;
import com.moveinsync.roster.dto.request.VehicleRequest;
import com.moveinsync.roster.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Scenario 19a: Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Scenario 19b: Insufficient role (EMPLOYEE calling ADMIN endpoint) returns 403 Forbidden")
    void employeeForbiddenFromAdminEndpoint() throws Exception {
        String employeeToken = jwtTokenProvider.generateToken("test_employee", "EMPLOYEE");

        VehicleRequest vehicleRequest = new VehicleRequest();
        vehicleRequest.setRegistrationNumber("KA05CD1234");
        vehicleRequest.setVehicleType("CAB");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 18: Invalid request returns meaningful validation errors and does not expose stack traces")
    void invalidDriverRequestReturnsValidationErrors() throws Exception {
        String adminToken = jwtTokenProvider.generateToken("admin", "ADMIN");

        // Request with missing name, missing phone, missing licence
        DriverRequest request = new DriverRequest();

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Scenario 20: Duplicate vehicle registration returns 409 Conflict")
    void duplicateVehicleRegistrationReturnsConflict() throws Exception {
        String adminToken = jwtTokenProvider.generateToken("admin", "ADMIN");

        VehicleRequest request1 = new VehicleRequest();
        request1.setRegistrationNumber("KA01DUP9999");
        request1.setVehicleType("CAB");

        // First creation succeeds
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second creation with same registration number returns 409 Conflict
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("Authentication: Login with valid credentials returns JWT token")
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Authentication: Login with invalid credentials returns 401 Unauthorized")
    void loginWithInvalidCredentialsReturns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "wrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
