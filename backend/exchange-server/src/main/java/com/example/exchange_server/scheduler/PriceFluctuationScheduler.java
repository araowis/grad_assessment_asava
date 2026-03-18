package com.example.exchange_server.scheduler;

import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.exchange_server.client.CompanyClient;

import com.example.exchange_server.dto.CompanyDTO;

@Component
public class PriceFluctuationScheduler {

    private final CompanyClient companyClient;
    private final Random random = new Random();

    public PriceFluctuationScheduler(CompanyClient companyClient) {
        this.companyClient = companyClient;
    }

    // Run every 10 seconds (adjust as needed)
    @Scheduled(fixedRate = 1000)
    public void fluctuatePrices() {
        List<CompanyDTO> companies = companyClient.getAllCompanies();

        for (CompanyDTO company : companies) {
            double currentPrice = company.getCurrentPrice();
            double openingPrice = company.getOpeningPrice();

            // Random percentage between -0.5% to +0.5%
            double changePercent = (random.nextDouble() - 0.5) / 100.0;
            double newPrice = currentPrice + currentPrice * changePercent;

            // Ensure daily deviation <= 20%
            double maxPrice = openingPrice * 1.20;
            double minPrice = openingPrice * 0.80;
            newPrice = Math.max(Math.min(newPrice, maxPrice), minPrice);

            // Update via CompanyClient
            companyClient.updatePrice(company.getShortId(), newPrice);
        }
    }
}
