package com.asava.trading.api_gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import com.asava.trading.api_gateway.filters.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final String secret = "dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkxMjM0NTY3ODk=";

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}