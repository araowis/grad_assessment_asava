package com.asava.trading.auth_service.service;

import com.asava.trading.auth_service.dto.*;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    TokenValidationResponse validateToken(String token);
    void logout(String username);
    void changePassword(String username, ChangePasswordRequest request);
}