package com.example.portfolio_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortfolioResponseDto {

    private Long id;
    private Long userId;
    private String companyId;
    private int quantity;
    private double averageBuyPrice;
    private double currentPrice;
    private double totalValue;
}
