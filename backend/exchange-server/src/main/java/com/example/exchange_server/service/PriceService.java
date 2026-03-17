package com.example.exchange_server.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.exchange_server.client.CompanyClient;

@Service
public class PriceService {

    @Autowired
    private CompanyClient companyClient;

    public void updatePrices() {
        List<CompanyDTO> companies = companyClient.getAllCompanies();

        for (CompanyDTO company : companies) {
            double newPrice = PriceUtil.calculateNewPrice(company.getPrice());
            companyClient.updatePrice(company.getShortId(), newPrice);
        }
    }
}
