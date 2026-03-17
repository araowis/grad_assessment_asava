package com.example.exchange_server.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TradeResponseDTO {

    private Long id;

    private Long buyerId;
    private Long sellerId;

    private String companyId;

    private int quantity;
    private double price;

    private LocalDateTime executedAt;
}
