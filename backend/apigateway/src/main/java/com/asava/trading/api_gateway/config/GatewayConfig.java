package com.asava.trading.api_gateway.config;

import java.util.List;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("auth-service", r -> r
                                                .path("/api/v1/auth/**")
                                                .uri("lb://AUTH-SERVICE"))

                                .route("company-service", r -> r
                                                .path("/api/v1/companies/**")
                                                .uri("lb://COMPANY-SERVICE"))

                                .route("exchange-server-service", r -> r
                                                .path("/api/v1/exchange/**")
                                                .uri("lb://EXCHANGE-SERVER-SERVICE"))

                                .build();
        }
}