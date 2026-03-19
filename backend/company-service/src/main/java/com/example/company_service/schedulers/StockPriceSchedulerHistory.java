package com.example.company_service.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.company_service.service.implementation.CompanyService;
import com.example.company_service.service.implementation.HistoryService;

@Component
public class StockPriceSchedulerHistory {
 
    @Autowired
    private HistoryService historyService;
 
    // Runs every 60 seconds (60,000 milliseconds)
    
}
