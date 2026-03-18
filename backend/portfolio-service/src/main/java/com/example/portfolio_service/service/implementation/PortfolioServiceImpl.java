package com.example.portfolio_service.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfolio_service.client.AuthClient;
import com.example.portfolio_service.client.CompanyClient;
import com.example.portfolio_service.client.ExchangeClient;
import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;
import com.example.portfolio_service.entity.Portfolio;
import com.example.portfolio_service.repository.PortfolioRepository;
import com.example.portfolio_service.service.IPortfolioService;

@Service
@Transactional
public class PortfolioServiceImpl implements IPortfolioService {

    private final PortfolioRepository repository;
    private final AuthClient authClient;
    private final CompanyClient companyClient;
    private final ExchangeClient exchangeClient;

    public PortfolioServiceImpl(PortfolioRepository repository, AuthClient authClient,
            CompanyClient companyClient, ExchangeClient exchangeClient) {
        this.repository = repository;
        this.authClient = authClient;
        this.companyClient = companyClient;
        this.exchangeClient = exchangeClient;
    }

    @Override
    public PortfolioResponseDto buy(Long userId, PortfolioRequestDto request) {

        // ✅ Validate user
        if (!authClient.userExists(userId)) {
            throw new RuntimeException("User not found");
        }

        // ✅ Validate company
        if (!companyClient.companyExists(request.getCompanyId())) {
            throw new RuntimeException("Company not found");
        }

        Portfolio portfolio = repository
                .findByUserIdAndCompanyId(userId, request.getCompanyId())
                .orElse(null);

        if (portfolio == null) {
            portfolio = Portfolio.builder()
                    .userId(userId)
                    .companyId(request.getCompanyId())
                    .quantity(request.getQuantity())
                    .averageBuyPrice(request.getPrice())
                    .build();
        } else {
            int totalQty = portfolio.getQuantity() + request.getQuantity();

            double totalCost
                    = (portfolio.getQuantity() * portfolio.getAverageBuyPrice())
                    + (request.getQuantity() * request.getPrice());

            portfolio.setQuantity(totalQty);
            portfolio.setAverageBuyPrice(totalCost / totalQty);
        }

        Portfolio saved = repository.save(portfolio);
        return mapToDto(saved);
    }

    @Override
    public void sell(Long userId, String companyId, int quantity) {

        Portfolio portfolio = repository
                .findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        if (portfolio.getQuantity() < quantity) {
            throw new RuntimeException("Not enough shares");
        }

        portfolio.setQuantity(portfolio.getQuantity() - quantity);

        if (portfolio.getQuantity() == 0) {
            repository.delete(portfolio);
        } else {
            repository.save(portfolio);
        }
    }

    @Override
    public List<PortfolioResponseDto> getPortfolio(Long userId) {

        return repository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private PortfolioResponseDto mapToDto(Portfolio p) {

        double currentPrice = 0;

        try {
            currentPrice = exchangeClient.getPrice(p.getCompanyId());
        } catch (Exception e) {
            // fallback if exchange-server is down
        }

        return PortfolioResponseDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .companyId(p.getCompanyId())
                .quantity(p.getQuantity())
                .averageBuyPrice(p.getAverageBuyPrice())
                .currentPrice(currentPrice)
                .totalValue(currentPrice * p.getQuantity())
                .build();
    }
}
