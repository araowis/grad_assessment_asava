package com.example.portfolio_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDto {

    private String shortId;
    private String name;
    private int noOfShare;
    private double openingPrice;
    private double currentPrice;
    private double dailyStartPrice;
}
