package com.example.portfolio_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.portfolio_service.dto.OrderRequestDTO;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@FeignClient(name = "exchange-server")
public interface ExchangeClient {

    @PostMapping("/exchange/order")
    String placeOrder(@RequestBody OrderRequestDTO dto);
}
