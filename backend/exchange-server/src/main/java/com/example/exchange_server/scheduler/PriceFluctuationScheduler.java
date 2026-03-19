package com.example.exchange_server.scheduler;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.exchange_server.client.CompanyClient;
import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.dto.PriceUpdateDTO;

/**
 * Simulates natural market price drift for all listed companies.
 * Failures are logged and swallowed so a transient company-service outage
 * doesn't kill the scheduler thread (Spring won't restart a @Scheduled
 * method whose Runnable throws).
 */
@Component
public class PriceFluctuationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceFluctuationScheduler.class);

    private static final double MAX_TICK_PCT  = 0.005; // +- 0.5% per tick
    private static final double MAX_DAILY_PCT = 0.20;  // +- 20% from opening price

    private final CompanyClient companyClient;
    private final Random random = new Random();

    public PriceFluctuationScheduler(CompanyClient companyClient) {
        this.companyClient = companyClient;
    }

    @Scheduled(fixedRate = 5_000)
    public void fluctuatePrices() {
        List<CompanyDTO> companies;
        try {
            companies = companyClient.getAllCompanies();
        } catch (Exception e) {
            log.error("Failed to fetch companies for price fluctuation: {}", e.getMessage());
            return;
        }

        if (companies == null || companies.isEmpty()) {
            return;
        }

        // Compute all new prices locally
        List<PriceUpdateDTO> updates = companies.stream()
                .map(this::computeUpdate)
                .collect(Collectors.toList());

        // One Feign call for the entire batch.
        try {
            companyClient.batchUpdatePrices(updates);
            log.debug("Batch price update sent for {} companies", updates.size());
        } catch (Exception e) {
            log.error("Batch price update failed: {}", e.getMessage());
        }
    }

    /**
     * Computes the next simulated price for one company.
     * No I/O
     */
    private PriceUpdateDTO computeUpdate(CompanyDTO company) {
        double current = company.getCurrentPrice();
        double opening = company.getOpeningPrice();

        double changePercent = (random.nextDouble() - 0.5) * 2 * MAX_TICK_PCT;
        double newPrice = current + current * changePercent;

        // Hard-clamp to the daily circuit-breaker band.
        double upper = opening * (1.0 + MAX_DAILY_PCT);
        double lower = opening * (1.0 - MAX_DAILY_PCT);
        newPrice = Math.max(lower, Math.min(upper, newPrice));

        return new PriceUpdateDTO(company.getShortId(), newPrice);
    }
}