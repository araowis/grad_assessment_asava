package com.example.portfolio_service.dto;

import lombok.Data;

@Data
public class PortfolioRequestDto {

    private Long portfolioGroupId;
    private String companyId;
    private int quantity;// optional (can remove later)
}
