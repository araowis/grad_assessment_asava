package com.example.portfolio_service.dto;

import lombok.Data;

@Data
public class PortfolioRequestDto {

    private Long portfolioGroupId;
    private String companyId;
    private double price;
    private String orderType;
    private int stopLoss;
    private int quantity;// optional (can remove later)
}
