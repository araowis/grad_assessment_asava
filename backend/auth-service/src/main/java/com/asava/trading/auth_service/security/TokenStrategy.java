package com.asava.trading.auth_service.security;

import com.asava.trading.auth_service.entity.User;

import java.util.Date;
import java.util.function.Function;

public interface TokenStrategy {

    String generateAccessToken(User user);

    boolean validateToken(String token);

    String extractUsername(String token);

    Long extractUserId(String token);

    String extractEmail(String token);

    String extractRole(String token);

    Date extractExpiration(String token);

    long getExpirationInSeconds();
}