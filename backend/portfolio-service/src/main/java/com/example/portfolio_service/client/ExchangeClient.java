package com.example.portfolio_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.portfolio_service.dto.OrderRequestDTO;
import com.example.portfolio_service.dto.OrderResponseDTO;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@FeignClient(name = "EXCHANGE-SERVER-SERVICE") // Use the exact name from Eureka
public interface ExchangeClient {

    @PostMapping("/api/v1/exchange/order") // Added leading slash
    OrderResponseDTO placeOrder(@RequestBody OrderRequestDTO dto); // Change String to DTO
}
