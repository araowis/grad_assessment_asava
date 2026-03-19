package com.example.company_service.service.implementation;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.company_service.models.Company;
import com.example.company_service.models.StockPriceHistory;
import com.example.company_service.repository.CompanyRepository;
import com.example.company_service.repository.HistoryRepository;

import jakarta.transaction.Transactional;

@Service
public class HistoryService {

    @Autowired private CompanyRepository companyRepo;
    @Autowired private HistoryRepository historyRepo;
    @Autowired private SseEmitterService sseEmitterService;

    @Transactional
    public void updateCompanyHistoryPrice(String id, Double newPrice, Company c) {
        c.setCurrentPrice(newPrice);

        StockPriceHistory history = new StockPriceHistory();
        history.setPrice(newPrice);
        history.setCompany(c);
        history.setRecordedAt(LocalDateTime.now());

        historyRepo.save(history);
        companyRepo.save(c);

        sseEmitterService.broadcast(id, newPrice);
    }
}