package com.example.company_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.company_service.models.StockPriceHistory;

public interface HistoryRepository extends JpaRepository<StockPriceHistory, Long> {
    Page<StockPriceHistory> findByCompanyShortIdOrderByRecordedAtAsc(String shortId, Pageable pageable);
}
