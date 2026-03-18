package com.example.company_service.dto;

public class CompanyDTO {

    private String shortId;
    private String name;
    private int noOfShare;
    private double openingPrice;
    private double currentPrice;

    // Default Constructor
    public CompanyDTO() {
    }

    // Parameterized Constructor
    public CompanyDTO(String shortId, String name, int noOfShare, double openingPrice, double currentPrice) {
        this.shortId = shortId;
        this.name = name;
        this.noOfShare = noOfShare;
        this.openingPrice = openingPrice;
        this.currentPrice = currentPrice;

        
    }

    // Getters & Setters
    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNoOfShare() {
        return noOfShare;
    }

    public void setNoOfShare(int noOfShare) {
        this.noOfShare = noOfShare;
    }

    public double getOpeningPrice() {
        return openingPrice;
    }

    public void setOpeningPrice(double openingPrice) {
        this.openingPrice = openingPrice;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
