package com.example.exchange_server.util;

import java.util.Random;

public class PriceUtil {

    public static double calculateNewPrice(double currentPrice) {
        double min = 0.005;
        double max = 0.2;

        double change = min + (max - min) * new Random().nextDouble();
        if (new Random().nextBoolean()) {
            change *= -1;
        }

        return currentPrice * (1 + change);
    }

}
