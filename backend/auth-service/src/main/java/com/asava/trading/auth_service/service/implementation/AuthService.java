package com.asava.trading.auth_service.service.implementation;

import com.asava.trading.auth_service.service.IAuthService;

import com.asava.trading.auth_service.dto.*;
import com.asava.trading.auth_service.entity.RefreshToken;
import com.asava.trading.auth_service.entity.User;
import com.asava.trading.auth_service.exception.*;
import com.asava.trading.auth_service.observability.implementation.AuditService;
import com.asava.trading.auth_service.repository.UserRepository;
import com.asava.trading.auth_service.security.TokenStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenStrategy tokenStrategy;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.ROLE_TRADER)
                // In production, set PENDING_VERIFICATION and send email
                // for now it's just auto activated :)
                .status(User.AccountStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} ({})", user.getUsername(), user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Load user first so we can track failed attempts
        User user = userRepository
                .findByEmail(request.getEmailOrUsername())
                .or(() -> userRepository.findByUsername(request.getEmailOrUsername()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check account lock
        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException(
                    "Account is locked. Try again after " + user.getLockedUntil());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            handleFailedAttempt(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        userRepository.resetFailedAttempts(user.getId());
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        auditService.loginSuccess(user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken existingToken =
                refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = existingToken.getUser();

        // Rotate: invalidate old, issue new
        refreshTokenService.revokeAllUserTokens(user);

        return buildAuthResponse(user);
    }

    // Token Validation for API Gateway

    public TokenValidationResponse validateToken(String token) {
        if (!tokenStrategy.validateToken(token)) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Token is invalid or expired")
                    .build();
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(tokenStrategy.extractUserId(token))
                .username(tokenStrategy.extractUsername(token))
                .email(tokenStrategy.extractEmail(token))
                .role(tokenStrategy.extractRole(token))
                .build();
    }

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(
                refreshTokenService::revokeAllUserTokens);
        log.info("User logged out: {}", username);
    }


    private AuthResponse buildAuthResponse(User user) {
        String accessToken = tokenStrategy.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(tokenStrategy.getExpirationInSeconds())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        userRepository.incrementFailedAttempts(user.getId());

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            userRepository.lockAccount(user.getId(), lockUntil);
            log.warn("Account locked for user {} after {} failed attempts", 
                     user.getUsername(), attempts);
        }
    }
}