package com.asava.trading.auth_service.service;

import com.asava.trading.auth_service.dto.*;
import com.asava.trading.auth_service.entity.RefreshToken;
import com.asava.trading.auth_service.entity.User;
import com.asava.trading.auth_service.exception.AccountLockedException;
import com.asava.trading.auth_service.exception.InvalidTokenException;
import com.asava.trading.auth_service.exception.UserAlreadyExistsException;
import com.asava.trading.auth_service.repository.UserRepository;
import com.asava.trading.auth_service.security.JwtTokenProvider;
import com.asava.trading.auth_service.service.implementation.AuthService;
import com.asava.trading.auth_service.service.implementation.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private User activeUser;
    private RefreshToken sampleRefreshToken;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("trader01")
                .email("trader@test.com")
                .password("encodedPassword")
                .fullName("Test Trader")
                .phoneNumber("+911234567890")
                .role(User.Role.ROLE_TRADER)
                .status(User.AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();

        sampleRefreshToken = RefreshToken.builder()
                .id(1L)
                .user(activeUser)
                .token("sample-refresh-uuid")
                .expiryDate(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("trader01");
        registerRequest.setEmail("trader@test.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setFullName("Test Trader");
        registerRequest.setPhoneNumber("+911234567890");

        loginRequest = new LoginRequest();
        loginRequest.setEmailOrUsername("trader@test.com");
        loginRequest.setPassword("Password@123");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Stubs the token-generation calls used in buildAuthResponse() */
    private void stubTokenGeneration() {
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(sampleRefreshToken);
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_ShouldReturnAuthResponse_WhenNewUser() {
        when(userRepository.existsByEmail("trader@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("trader01")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        stubTokenGeneration();

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("sample-refresh-uuid", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrow_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("trader@test.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrow_WhenUsernameAlreadyTaken() {
        when(userRepository.existsByEmail("trader@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("trader01")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldEncodePassword_BeforeSaving() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        stubTokenGeneration();

        authService.register(registerRequest);

        // Raw password must NEVER be persisted
        verify(passwordEncoder).encode("Password@123");
    }

    @Test
    void register_ShouldSaveEmail_AsLowerCase() {
        registerRequest.setEmail("TRADER@TEST.COM");

        when(userRepository.existsByEmail("TRADER@TEST.COM")).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertEquals("trader@test.com", saved.getEmail(),
                    "Email must be lowercased before saving");
            return activeUser;
        });
        stubTokenGeneration();

        authService.register(registerRequest);
    }

    @Test
    void register_ShouldAssignRoleTrader_ByDefault() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertEquals(User.Role.ROLE_TRADER, saved.getRole(),
                    "New users must default to ROLE_TRADER");
            return activeUser;
        });
        stubTokenGeneration();

        authService.register(registerRequest);
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("trader@test.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        stubTokenGeneration();

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(userRepository).resetFailedAttempts(activeUser.getId());
        verify(userRepository).updateLastLogin(eq(activeUser.getId()), any(LocalDateTime.class));
    }

    @Test
    void login_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("trader@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("trader@test.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    void login_ShouldThrow_WhenAccountIsLocked() {
        // lockedUntil in the future → isAccountNonLocked() returns false
        User lockedUser = User.builder()
                .id(2L)
                .username("trader01")
                .email("trader@test.com")
                .password("encoded")
                .fullName("Locked Trader")
                .role(User.Role.ROLE_TRADER)
                .status(User.AccountStatus.ACTIVE)
                .lockedUntil(LocalDateTime.now().plusMinutes(20))
                .build();

        when(userRepository.findByEmail("trader@test.com")).thenReturn(Optional.of(lockedUser));

        assertThrows(AccountLockedException.class,
                () -> authService.login(loginRequest));

        // Must never hit authenticationManager for a locked account
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldNotLockAccount_WhenLockHasExpired() {
        // lockedUntil is in the PAST → isAccountNonLocked() returns true
        User expiredLockUser = User.builder()
                .id(3L)
                .username("trader01")
                .email("trader@test.com")
                .password("encoded")
                .fullName("Recovered Trader")
                .role(User.Role.ROLE_TRADER)
                .status(User.AccountStatus.ACTIVE)
                .lockedUntil(LocalDateTime.now().minusMinutes(5))
                .build();

        when(userRepository.findByEmail("trader@test.com")).thenReturn(Optional.of(expiredLockUser));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        stubTokenGeneration();

        // Expired lock → login should succeed
        assertDoesNotThrow(() -> authService.login(loginRequest));
    }

    @Test
    void login_ShouldIncrementFailedAttempts_WhenPasswordIsWrong() {
        when(userRepository.findByEmail("trader@test.com")).thenReturn(Optional.of(activeUser));
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        verify(userRepository).incrementFailedAttempts(activeUser.getId());
        // Must NOT reset attempts on a failed login
        verify(userRepository, never()).resetFailedAttempts(any());
    }

    @Test
    void login_ShouldLockAccount_WhenFailedAttemptsReachMax() {
        // User is at 4 failed attempts — next (5th) attempt triggers lock
        User almostLockedUser = User.builder()
                .id(3L)
                .username("trader01")
                .email("trader@test.com")
                .password("encoded")
                .fullName("Almost Locked")
                .role(User.Role.ROLE_TRADER)
                .status(User.AccountStatus.ACTIVE)
                .failedLoginAttempts(4)
                .build();

        when(userRepository.findByEmail("trader@test.com"))
                .thenReturn(Optional.of(almostLockedUser));
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        verify(userRepository).lockAccount(
                eq(almostLockedUser.getId()), any(LocalDateTime.class));
    }

    @Test
    void login_ShouldFindUserByUsername_WhenEmailLookupReturnsEmpty() {
        LoginRequest usernameLogin = new LoginRequest();
        usernameLogin.setEmailOrUsername("trader01");
        usernameLogin.setPassword("Password@123");

        when(userRepository.findByEmail("trader01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("trader01")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        stubTokenGeneration();

        AuthResponse response = authService.login(usernameLogin);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }

    // ─── refreshToken ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("sample-refresh-uuid");

        when(refreshTokenService.verifyRefreshToken("sample-refresh-uuid"))
                .thenReturn(sampleRefreshToken);
        doNothing().when(refreshTokenService).revokeAllUserTokens(activeUser);
        stubTokenGeneration();

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        // Token rotation: old tokens must be revoked before issuing new
        verify(refreshTokenService).revokeAllUserTokens(activeUser);
        verify(refreshTokenService).createRefreshToken(activeUser);
    }

    @Test
    void refreshToken_ShouldThrow_WhenRefreshTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("unknown-token");

        when(refreshTokenService.verifyRefreshToken("unknown-token"))
                .thenThrow(new InvalidTokenException("Refresh token not found"));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));

        assertEquals("Refresh token not found", ex.getMessage());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    void refreshToken_ShouldThrow_WhenRefreshTokenIsRevoked() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        when(refreshTokenService.verifyRefreshToken("revoked-token"))
                .thenThrow(new InvalidTokenException("Refresh token has been revoked"));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));

        assertEquals("Refresh token has been revoked", ex.getMessage());
    }

    @Test
    void refreshToken_ShouldThrow_WhenRefreshTokenIsExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenService.verifyRefreshToken("expired-token"))
                .thenThrow(new InvalidTokenException("Refresh token has expired. Please log in again."));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));

        assertTrue(ex.getMessage().contains("expired"));
    }

    // ─── validateToken ────────────────────────────────────────────────────────

    @Test
    void validateToken_ShouldReturnValidResponse_WhenTokenIsGood() {
        when(jwtTokenProvider.validateToken("valid-jwt")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("valid-jwt")).thenReturn(1L);
        when(jwtTokenProvider.extractUsername("valid-jwt")).thenReturn("trader01");
        when(jwtTokenProvider.extractEmail("valid-jwt")).thenReturn("trader@test.com");
        when(jwtTokenProvider.extractRole("valid-jwt")).thenReturn("ROLE_TRADER");

        TokenValidationResponse response = authService.validateToken("valid-jwt");

        assertTrue(response.isValid());
        assertEquals(1L, response.getUserId());
        assertEquals("trader01", response.getUsername());
        assertEquals("trader@test.com", response.getEmail());
        assertEquals("ROLE_TRADER", response.getRole());
    }

    @Test
    void validateToken_ShouldReturnInvalidResponse_WhenTokenIsBad() {
        when(jwtTokenProvider.validateToken("bad-jwt")).thenReturn(false);

        TokenValidationResponse response = authService.validateToken("bad-jwt");

        assertFalse(response.isValid());
        assertEquals("Token is invalid or expired", response.getMessage());
        // Must NOT attempt claim extraction from an invalid token
        verify(jwtTokenProvider, never()).extractUserId(any());
        verify(jwtTokenProvider, never()).extractUsername(any());
        verify(jwtTokenProvider, never()).extractEmail(any());
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_ShouldRevokeAllTokens_WhenUserExists() {
        when(userRepository.findByUsername("trader01")).thenReturn(Optional.of(activeUser));
        doNothing().when(refreshTokenService).revokeAllUserTokens(activeUser);

        assertDoesNotThrow(() -> authService.logout("trader01"));

        verify(refreshTokenService).revokeAllUserTokens(activeUser);
    }

    @Test
    void logout_ShouldDoNothing_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Logging out a non-existent user is a no-op — must never throw
        assertDoesNotThrow(() -> authService.logout("ghost"));

        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }
}