package com.example.company_service.dto;

import java.time.LocalDateTime;

public record StockHistoryDTO(Double price, LocalDateTime recordedAt) {}
