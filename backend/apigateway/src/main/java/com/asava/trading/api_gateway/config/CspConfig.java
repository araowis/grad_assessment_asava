package com.asava.trading.api_gateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public SecurityFilterChain CspConfig(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives(
                    "default-src 'self'; " +
                    "connect-src 'self' http://localhost:8080 http://localhost:4200"
                )
            )
        );

    return http.build();
}