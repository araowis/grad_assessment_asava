package com.example.portfolio_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/v1/companies/{id}/exists")
    boolean getCompanyById(@PathVariable("id") String id);

    @GetMapping("/api/v1/companies/{id}")
    Object getCompanyData(@PathVariable("id") String id);
}
