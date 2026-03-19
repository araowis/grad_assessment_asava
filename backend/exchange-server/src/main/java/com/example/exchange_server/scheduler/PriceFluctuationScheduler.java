package com.example.exchange_server.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.exchange_server.client.CompanyClient;
import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.dto.PriceUpdateDTO;

import jakarta.annotation.PostConstruct;

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

    private final Map<String, Double> livePrices = new ConcurrentHashMap<>();
    private final Map<String, Double> openingPrices = new ConcurrentHashMap<>();

    public PriceFluctuationScheduler(CompanyClient companyClient) {
        this.companyClient = companyClient;
    }

    @PostConstruct
    public void seedPrices() {
        try {
            List<CompanyDTO> companies = companyClient.getAllCompanies();
            if (companies != null) {
                companies.forEach(c -> {
                    livePrices.put(c.getShortId(), c.getCurrentPrice());
                    openingPrices.put(c.getShortId(), c.getOpeningPrice());
                    log.info("Seeded {} at opening={} current={}", 
                        c.getShortId(), c.getOpeningPrice(), c.getCurrentPrice());
                });
            }
        } catch (Exception e) {
            log.error("Failed to seed prices on startup: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5_000)
    public void fluctuatePrices() {
        if (livePrices.isEmpty()) {
            log.warn("No prices seeded yet, retrying seed...");
            seedPrices();
            return;
        }

        // Compute all new prices from in-memory state — no fetch needed
        List<PriceUpdateDTO> updates = livePrices.entrySet().stream()
                .map(entry -> computeUpdate(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Update in-memory state immediately
        updates.forEach(u -> livePrices.put(u.getShortId(), u.getNewPrice()));

        // Tell company-service to record it
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
    private PriceUpdateDTO computeUpdate(String shortId, double current) {
        double opening = openingPrices.getOrDefault(shortId, current);

        double changePercent = (random.nextDouble() - 0.5) * 2 * MAX_TICK_PCT;
        double newPrice = current + current * changePercent;

        double upper = opening * (1.0 + MAX_DAILY_PCT);
        double lower = opening * (1.0 - MAX_DAILY_PCT);
        newPrice = Math.max(lower, Math.min(upper, newPrice));

        return new PriceUpdateDTO(shortId, newPrice);
    }
}