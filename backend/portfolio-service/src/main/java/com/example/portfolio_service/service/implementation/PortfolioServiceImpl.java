package com.example.portfolio_service.service.implementation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.portfolio_service.client.CompanyClient;
import com.example.portfolio_service.client.ExchangeClient;
import com.example.portfolio_service.dto.*;
import com.example.portfolio_service.entity.Portfolio;
import com.example.portfolio_service.repository.PortfolioRepository;
import com.example.portfolio_service.service.IPortfolioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements IPortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ExchangeClient exchangeClient;
    private final CompanyClient companyClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PortfolioResponseDto buy(Long userId, PortfolioRequestDto request) {
        // 1. Call Exchange
        OrderRequestDTO order = new OrderRequestDTO();
        order.setUserId(userId);
        order.setCompanyId(request.getCompanyId());
        order.setQuantity(request.getQuantity());
        order.setOrderType("BUY");

        Object result = exchangeClient.placeOrder(order);
        OrderResponseDTO response = objectMapper.convertValue(result, OrderResponseDTO.class);

        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Order failed in exchange");
        }

        double price = response.getExecutedPrice();

        // 2. Find using portfolioGroupId (FIXED NAME)
        Portfolio portfolio = portfolioRepository
                .findByUserIdAndPortfolioGroupIdAndCompanyId(
                        userId,
                        request.getPortfolioGroupId(),
                        request.getCompanyId())
                .orElse(null);

        if (portfolio == null) {
            portfolio = Portfolio.builder()
                    .userId(userId)
                    .portfolioGroupId(request.getPortfolioGroupId())
                    .companyId(request.getCompanyId())
                    .quantity(request.getQuantity())
                    .averageBuyPrice(price)
                    .build();
        } else {
            int oldQty = portfolio.getQuantity();
            double oldAvg = portfolio.getAverageBuyPrice();
            int newQty = oldQty + request.getQuantity();
            double newAvg = ((oldQty * oldAvg) + (request.getQuantity() * price)) / newQty;

            portfolio.setQuantity(newQty);
            portfolio.setAverageBuyPrice(newAvg);
        }

        return mapToDto(portfolioRepository.save(portfolio));
    }

    @Transactional
    @Override
    public PortfolioResponseDto sell(Long userId, PortfolioRequestDto request) {
        Portfolio portfolio = portfolioRepository
                .findByUserIdAndPortfolioGroupIdAndCompanyId(
                        userId,
                        request.getPortfolioGroupId(),
                        request.getCompanyId()
                )
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (portfolio.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient shares");
        }

        // Call Exchange
        OrderRequestDTO order = new OrderRequestDTO();
        order.setUserId(userId);
        order.setCompanyId(request.getCompanyId());
        order.setQuantity(request.getQuantity());
        order.setOrderType("SELL");

        Object result = exchangeClient.placeOrder(order);
        OrderResponseDTO response = objectMapper.convertValue(result, OrderResponseDTO.class);

        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Sell failed");
        }

        int remainingQty = portfolio.getQuantity() - request.getQuantity();

        if (remainingQty == 0) {
            portfolioRepository.delete(portfolio);
            return PortfolioResponseDto.builder()
                    .companyId(request.getCompanyId())
                    .quantity(0)
                    .averageBuyPrice(0.0)
                    .build();
        }

        portfolio.setQuantity(remainingQty);
        return mapToDto(portfolioRepository.save(portfolio));
    }

    @Override
    public List<PortfolioResponseDto> getPortfolio(Long userId, Long portfolioId, String companyId) {
        return portfolioRepository
                .findByUserIdAndPortfolioGroupIdAndCompanyId(userId, portfolioId, companyId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkAndExecuteStopLoss() {
        List<Portfolio> activeTriggers = portfolioRepository.findByStopLossPriceIsNotNull();

        for (Portfolio p : activeTriggers) {
            try {
                // 1. Get the company object
                Object companyData = companyClient.getCompanyData(p.getCompanyId());

                // 2. Extract currentPrice (assuming the field name in Company entity is currentPrice)
                // We convert the Object to a Map to read the value dynamically
                java.util.Map<String, Object> map = objectMapper.convertValue(companyData, java.util.Map.class);
                Double currentPrice = Double.valueOf(map.get("currentPrice").toString());

                // 3. Check and Sell
                if (currentPrice <= p.getStopLossPrice()) {
                    PortfolioRequestDto sellRequest = new PortfolioRequestDto();
                    sellRequest.setCompanyId(p.getCompanyId());
                    sellRequest.setQuantity(p.getQuantity());
                    sellRequest.setPortfolioGroupId(p.getPortfolioGroupId());

                    this.sell(p.getUserId(), sellRequest);

                    p.setStopLossPrice(null);
                    portfolioRepository.save(p);
                }
            } catch (Exception e) {
                System.err.println("Stop-loss check failed for: " + p.getCompanyId() + " - " + e.getMessage());
            }
        }
    }

    private PortfolioResponseDto mapToDto(Portfolio portfolio) {
        return PortfolioResponseDto.builder()
                .companyId(portfolio.getCompanyId())
                .quantity(portfolio.getQuantity())
                .averageBuyPrice(portfolio.getAverageBuyPrice())
                .build();
    }
}
