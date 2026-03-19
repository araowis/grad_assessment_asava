// package com.asava.trading.api_gateway.config;
// import java.util.List;
// import org.springframework.cloud.gateway.route.RouteLocator;
// import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// @Configuration
// public class GatewayConfig {
//         @Bean
//         public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//                 return builder.routes()
//                                 .route("auth-service", r -> r
//                                                 .path("/api/v1/auth/**")
//                                                 .uri("lb://AUTH-SERVICE"))
//                                 .route("company-service", r -> r
//                                                 .path("/api/v1/companies/**")
//                                                 .uri("lb://COMPANY-SERVICE"))
//                                 .route("exchange-server-service", r -> r
//                                                 .path("/api/v1/exchange/**")
//                                                 .uri("lb://EXCHANGE-SERVER-SERVICE"))
//                                 .build();
//         }
// }
package com.asava.trading.api_gateway.config;

import java.util.List;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri("lb://AUTH-SERVICE"))
                // 🟢 ADD THIS SPECIFIC ROUTE
                // .route("company-stats", r -> r
                // .path("/api/v1/stats")
                // .uri("lb://COMPANY-SERVICE"))
                .route("company-service", r -> r
                .path("/api/v1/companies/**")
                .uri("lb://COMPANY-SERVICE"))
                .route("exchange-server-service", r -> r
                .path("/api/v1/exchange/**")
                .uri("lb://EXCHANGE-SERVER-SERVICE"))
                .route("portfolio-service", r -> r
                .path("/api/v1/portfolios/**")
                .uri("lb://PORTFOLIO-SERVICE"))
                .build();
    }

    // 🟢 CORS FIX: This replaces all CORS code in your microservices
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
