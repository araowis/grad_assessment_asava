package com.example.exchange_server.dto;

import lombok.Data;

@Data
public class CompanyDTO {

    private String shortId;   // same as company-service

    private String name;

    private int noOfShare;

    private double openingPrice;
    private double currentPrice;

    public CompanyDTO(String shortId, String name, int noOfShare, double openingPrice, double currentPrice) {
        this.shortId = shortId;
        this.name = name;
        this.noOfShare = noOfShare;
        this.openingPrice = openingPrice;
        this.currentPrice = currentPrice;
    }
}
