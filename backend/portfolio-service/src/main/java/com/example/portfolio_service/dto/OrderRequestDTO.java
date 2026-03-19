package com.example.portfolio_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderRequestDTO {

    private Long userId;
    private String companyId;
    private int quantity;
    private double price;
    private String orderType; // BUY or SELL
}
