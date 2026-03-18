package com.asava.trading.auth_service.controller;

import com.asava.trading.auth_service.dto.*;
import com.asava.trading.auth_service.exception.AccountLockedException;
import com.asava.trading.auth_service.exception.InvalidTokenException;
import com.asava.trading.auth_service.exception.UserAlreadyExistsException;
import com.asava.trading.auth_service.service.IAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IAuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthResponse sampleAuthResponse;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    // ─── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(1L)
                .username("trader01")
                .email("trader@test.com")
                .fullName("Test Trader")
                .role("ROLE_TRADER")
                .build();

        sampleAuthResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .user(userInfo)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("trader01");
        registerRequest.setEmail("trader@test.com");
        registerRequest.setPassword("Password@123!");
        registerRequest.setFullName("Test Trader");
        registerRequest.setPhoneNumber("+911234567890");

        loginRequest = new LoginRequest();
        loginRequest.setEmailOrUsername("trader@test.com");
        loginRequest.setPassword("Password@123!");
    }

    // ─── POST /api/v1/auth/register ───────────────────────────────────────────

    @Test
    void register_ShouldReturn201_WhenRegistrationSuccessful() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void register_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    // ─── POST /api/v1/auth/login ──────────────────────────────────────────────

    @Test
    void login_ShouldReturn200_WhenCredentialsAreValid() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void login_ShouldReturn401_WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn403_WhenAccountIsLocked() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccountLockedException("Account is locked. Try again later."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/v1/auth/refresh-token ─────────────────────────────────────

    @Test
    void refreshToken_ShouldReturn200_WhenTokenIsValid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void refreshToken_ShouldReturn400_WhenTokenIsInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Refresh token not found"));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/v1/auth/validate?token= ────────────────────────────────────

    @Test
    void validateToken_ShouldReturn200_WhenTokenIsValid() throws Exception {
        TokenValidationResponse validResponse = TokenValidationResponse.builder()
                .valid(true)
                .userId(1L)
                .username("trader01")
                .email("trader@test.com")
                .role("ROLE_TRADER")
                .build();

        when(authService.validateToken("valid-jwt")).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .param("token", "valid-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("trader01"))
                .andExpect(jsonPath("$.role").value("ROLE_TRADER"));
    }

    @Test
    void validateToken_ShouldReturn401_WhenTokenIsInvalid() throws Exception {
        TokenValidationResponse invalidResponse = TokenValidationResponse.builder()
                .valid(false)
                .message("Token is invalid or expired")
                .build();

        when(authService.validateToken("bad-jwt")).thenReturn(invalidResponse);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .param("token", "bad-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    // ─── POST /api/v1/auth/logout ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "trader01")
    void logout_ShouldReturn200_WhenUserIsAuthenticated() throws Exception {
        doNothing().when(authService).logout("trader01");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("trader01");
    }
}