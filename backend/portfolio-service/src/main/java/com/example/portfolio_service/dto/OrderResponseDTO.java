package com.example.portfolio_service.dto;

import lombok.Data;

@Data
public class OrderResponseDTO {

    private double executedPrice;
    private String status; // SUCCESS / FAILED
}
