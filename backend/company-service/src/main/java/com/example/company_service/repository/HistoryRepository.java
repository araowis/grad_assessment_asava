package com.example.company_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.company_service.models.StockPriceHistory;

public interface HistoryRepository extends JpaRepository<StockPriceHistory, Long> {

}
