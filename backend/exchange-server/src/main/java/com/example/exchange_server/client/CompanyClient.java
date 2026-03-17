package com.example.exchange_server.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.exchange_server.dto.CompanyDTO;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/companies")
    List<CompanyDTO> getAllCompanies();

    @GetMapping("/companies/{id}")
    CompanyDTO getCompanyById(@PathVariable String id);

    @PutMapping("/companies/{id}/price")
    void updatePrice(@PathVariable String id, @RequestParam double price);
}
