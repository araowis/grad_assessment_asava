package com.example.portfolio_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.portfolio_service.dto.CompanyResponseDto;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/v1/companies/{id}")
    CompanyResponseDto getCompanyData(@PathVariable("id") String id);

    @GetMapping("/api/v1/companies")
    List<CompanyResponseDto> getAllCompanies();
}
