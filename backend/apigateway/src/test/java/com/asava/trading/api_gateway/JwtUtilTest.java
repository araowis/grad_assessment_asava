package com.asava.trading.api_gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.asava.trading.api_gateway.utils.JwtUtil;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Same secret as JwtUtil — must match exactly
    private static final String SECRET =
            "dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkxMjM0NTY3ODk=";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a valid signed JWT with the same key JwtUtil uses */
    private String buildToken(Map<String, Object> claims, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .claims(claims)
                .subject("trader01")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    private String validToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "ROLE_TRADER");
        claims.put("email", "trader@test.com");
        return buildToken(claims, 60_000); // expires in 60 seconds
    }

    private String expiredToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "ROLE_TRADER");
        return buildToken(claims, -1000); // already expired
    }

    // ─── validate ─────────────────────────────────────────────────────────────

    @Test
    void validate_ShouldReturnTrue_WhenTokenIsValid() {
        assertTrue(jwtUtil.validate(validToken()));
    }

    @Test
    void validate_ShouldReturnFalse_WhenTokenIsExpired() {
        assertFalse(jwtUtil.validate(expiredToken()));
    }

    @Test
    void validate_ShouldReturnFalse_WhenTokenIsMalformed() {
        assertFalse(jwtUtil.validate("this.is.not.a.jwt"));
    }

    @Test
    void validate_ShouldReturnFalse_WhenTokenIsEmpty() {
        assertFalse(jwtUtil.validate(""));
    }

    @Test
    void validate_ShouldReturnFalse_WhenTokenIsNull() {
        assertFalse(jwtUtil.validate(null));
    }

    @Test
    void validate_ShouldReturnFalse_WhenTokenSignatureIsTampered() {
        String validJwt = validToken();
        // Tamper the signature (last segment)
        String[] parts = validJwt.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignatureXYZ";
        assertFalse(jwtUtil.validate(tampered));
    }

    // ─── extractClaims ────────────────────────────────────────────────────────

    @Test
    void extractClaims_ShouldReturnCorrectUserId() {
        Claims claims = jwtUtil.extractClaims(validToken());
        assertNotNull(claims.get("userId"));
        assertEquals(1, ((Number) claims.get("userId")).intValue());
    }

    @Test
    void extractClaims_ShouldReturnCorrectRole() {
        Claims claims = jwtUtil.extractClaims(validToken());
        assertEquals("ROLE_TRADER", claims.get("role"));
    }

    @Test
    void extractClaims_ShouldReturnCorrectEmail() {
        Claims claims = jwtUtil.extractClaims(validToken());
        assertEquals("trader@test.com", claims.get("email"));
    }

    @Test
    void extractClaims_ShouldReturnCorrectSubject() {
        Claims claims = jwtUtil.extractClaims(validToken());
        assertEquals("trader01", claims.getSubject());
    }

    @Test
    void extractClaims_ShouldThrow_WhenTokenIsExpired() {
        assertThrows(Exception.class,
                () -> jwtUtil.extractClaims(expiredToken()),
                "Extracting claims from an expired token must throw");
    }
}