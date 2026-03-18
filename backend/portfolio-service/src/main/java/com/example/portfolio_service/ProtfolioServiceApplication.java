package com.example.portfolio_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ProtfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtfolioServiceApplication.class, args);
    }

}
