package com.example.exchange_server.util;

import org.springframework.stereotype.Component;

@Component
public class ValidatePrice {

    public double validatePrice(double oldPrice, double newPrice, double openingPrice) {

        double minChange = oldPrice * 0.005; // 0.5%
        double maxUpper = openingPrice * 1.20;
        double maxLower = openingPrice * 0.80;

        // Ensure minimum fluctuation
        if (Math.abs(newPrice - oldPrice) < minChange) {
            throw new RuntimeException("Price change too small (<0.5%)");
        }

        // Ensure max daily cap
        if (newPrice > maxUpper || newPrice < maxLower) {
            throw new RuntimeException("Price exceeds 20% daily limit");
        }

        return newPrice;
    }
}
