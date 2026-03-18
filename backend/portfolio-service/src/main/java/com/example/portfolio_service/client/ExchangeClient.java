package com.example.portfolio_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "exchange-server")
public interface ExchangeClient {

    @GetMapping("/api/prices/{companyId}")
    double getPrice(@PathVariable("companyId") String companyId);
}
