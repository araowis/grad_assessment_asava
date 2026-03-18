package com.asava.trading.api_gateway.filters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.asava.trading.api_gateway.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        log.info("Incoming request path: {}", path);

        // Skip auth endpoints
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            log.info("Public path accessed: {}, skipping JWT check", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null) {
            log.warn("Missing Authorization header for path: {}", path);
            return unauthorized(exchange);
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format for path: {} | Header: {}", path, authHeader);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);
        log.info("JWT received: {}", token);

        if (!jwtUtil.validate(token)) {
            log.warn("JWT validation failed for path: {} | Token: {}", path, token);
            return unauthorized(exchange);
        }

        Claims claims = jwtUtil.extractClaims(token);
        log.info("JWT validated successfully for userId: {} | role: {}", claims.get("userId"), claims.get("role"));

        // OPTIONAL: forward user info
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.get("userId").toString())
                .header("X-User-Role", claims.get("role").toString())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        log.warn("Returning 401 Unauthorized for request: {}", exchange.getRequest().getURI().getPath());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}