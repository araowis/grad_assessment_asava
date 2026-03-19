package com.example.exchange_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry in a batch price-update request to the company-service.
 * The scheduler builds a list of these and sends them in a single Feign call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateDTO {
    private String companyId;
    private double newPrice;
}