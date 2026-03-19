package com.example.company_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Company {

    @Id
    @Column(nullable = false, unique = true)
    private String shortId;

    @Column(nullable = false)
    private String name;

    @Column(name = "no_of_share")
    private int noOfShare;

    @Column(nullable = false, name = "opening_price")
    private double openingPrice = 0.0;

    @Column(nullable = false, name = "current_price")
    private double currentPrice = 0.0;

    public Company() {
    }

    public Company(String shortId, String name, int noOfShare, double openingPrice, double currentPrice) {
        this.shortId = shortId;
        this.name = name;
        this.noOfShare = noOfShare;
        this.openingPrice = openingPrice;
        this.currentPrice = currentPrice;
    }

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

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setOpeningPrice(double openingPrice) {
        this.openingPrice = openingPrice;
    }
    public double getOpeningPrice() {
        return openingPrice;
    }
}
