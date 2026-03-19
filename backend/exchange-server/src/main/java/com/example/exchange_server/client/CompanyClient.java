package com.example.exchange_server.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.dto.PriceUpdateDTO;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/v1/companies")
    List<CompanyDTO> getAllCompanies();

    @GetMapping("/api/v1/companies/{id}")
    CompanyDTO getCompanyById(@PathVariable String id);

    @PutMapping("/api/v1/companies/{id}/price")
    void updatePrice(@PathVariable String id, @RequestParam double price);

    /**
     * Batch update — used exclusively by PriceFluctuationScheduler.
     * Collapses N per-company Feign calls into one HTTP request per tick.
     * The company-service must expose PUT /api/v1/companies/prices.
     */
    @PutMapping("/api/v1/companies/prices")
    void batchUpdatePrices(@RequestBody List<PriceUpdateDTO> updates);
}
