package com.example.portfolio_service.dto;

import lombok.Data;

@Data
public class PortfolioRequestDto {

    private String companyId;
    private int quantity;
    private double price;
}
