package com.example.portfolio_service.dto;

import lombok.Data;

@Data
public class OrderRequestDTO {

    private Long userId;
    private String companyId;
    private int quantity;
    private String orderType; // BUY or SELL
}
