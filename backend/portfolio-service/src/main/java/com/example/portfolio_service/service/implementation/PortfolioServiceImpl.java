// package com.example.portfolio_service.service.implementation;
// import java.util.List;
// // import java.util.stream.Collectors;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import com.example.portfolio_service.client.CompanyClient;
// import com.example.portfolio_service.client.ExchangeClient;
// import com.example.portfolio_service.dto.CompanyResponseDto;
// import com.example.portfolio_service.dto.OrderRequestDTO;
// import com.example.portfolio_service.dto.OrderResponseDTO;
// import com.example.portfolio_service.dto.PortfolioRequestDto;
// import com.example.portfolio_service.dto.PortfolioResponseDto;
// import com.example.portfolio_service.entity.Portfolio;
// import com.example.portfolio_service.repository.PortfolioRepository;
// import com.example.portfolio_service.service.IPortfolioService;
// import lombok.RequiredArgsConstructor;
// @Service
// @RequiredArgsConstructor
// public class PortfolioServiceImpl implements IPortfolioService {
//     private final PortfolioRepository portfolioRepository;
//     private final ExchangeClient exchangeClient;
//     private final CompanyClient companyClient;
//     @Override
//     @Transactional
//     public PortfolioResponseDto buy(Long userId, PortfolioRequestDto request) {
//         // 1. Prepare Order for Exchange
//         OrderRequestDTO order = OrderRequestDTO.builder()
//                 .userId(userId)
//                 .companyId(request.getCompanyId())
//                 .quantity(request.getQuantity())
//                 .price(request.getPrice())
//                 .orderType("BUY")
//                 .build();
//         // 2. Execute via Exchange Service
//         OrderResponseDTO response = exchangeClient.placeOrder(order);
//         // Requirement: Handle Success/Rejected Status
//         if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
//             throw new RuntimeException("Order REJECTED: " + (response != null ? response.getStatus() : "Exchange Offline"));
//         }
//         double executedPrice = response.getExecutedPrice();
//         // 3. Update Portfolio logic
//         Portfolio portfolio = portfolioRepository
//                 .findByUserIdAndPortfolioGroupIdAndCompanyId(userId, request.getPortfolioGroupId(), request.getCompanyId())
//                 .orElse(Portfolio.builder()
//                         .userId(userId)
//                         .portfolioGroupId(request.getPortfolioGroupId())
//                         .companyId(request.getCompanyId())
//                         .quantity(0)
//                         .averageBuyPrice(0.0)
//                         .build());
//         // 4. Calculate Weighted Average: (Total Cost) / (Total Quantity)
//         int currentQty = portfolio.getQuantity();
//         double currentAvg = portfolio.getAverageBuyPrice();
//         int addedQty = request.getQuantity();
//         double newAvg = ((currentQty * currentAvg) + (addedQty * executedPrice)) / (currentQty + addedQty);
//         portfolio.setQuantity(currentQty + addedQty);
//         portfolio.setAverageBuyPrice(newAvg);
//         // 🟢 FIX: Removed extra parenthesis from your previous snippet
//         if (request.getStopLoss() > 0) {
//             portfolio.setStopLossPrice((double) request.getStopLoss());
//         }
//         return mapToDto(portfolioRepository.save(portfolio));
//     }
//     @Override
//     @Transactional
//     public PortfolioResponseDto sell(Long userId, PortfolioRequestDto request) {
//         Portfolio portfolio = portfolioRepository
//                 .findByUserIdAndPortfolioGroupIdAndCompanyId(userId, request.getPortfolioGroupId(), request.getCompanyId())
//                 .orElseThrow(() -> new RuntimeException("Asset not found in portfolio"));
//         if (portfolio.getQuantity() < request.getQuantity()) {
//             throw new RuntimeException("REJECTED: Insufficient shares to sell.");
//         }
//         OrderRequestDTO order = OrderRequestDTO.builder()
//                 .userId(userId)
//                 .companyId(request.getCompanyId())
//                 .quantity(request.getQuantity())
//                 .price(request.getPrice())
//                 .orderType("SELL")
//                 .build();
//         OrderResponseDTO response = exchangeClient.placeOrder(order);
//         if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
//             throw new RuntimeException("Sell order REJECTED by exchange.");
//         }
//         int remainingQty = portfolio.getQuantity() - request.getQuantity();
//         if (remainingQty <= 0) {
//             portfolioRepository.delete(portfolio);
//             return PortfolioResponseDto.builder().companyId(request.getCompanyId()).quantity(0).build();
//         } else {
//             portfolio.setQuantity(remainingQty);
//             return mapToDto(portfolioRepository.save(portfolio));
//         }
//     }
//     @Scheduled(fixedRate = 10000)
//     @Transactional
//     @Override
//     public void checkAndExecuteStopLoss() {
//         List<Portfolio> activeTriggers = portfolioRepository.findByStopLossPriceIsNotNull();
//         for (Portfolio p : activeTriggers) {
//             try {
//                 CompanyResponseDto company = companyClient.getCompanyData(p.getCompanyId());
//                 // Requirement: SafeLoss logic (Execute sell if market price <= trigger)
//                 if (company.getCurrentPrice() <= p.getStopLossPrice()) {
//                     PortfolioRequestDto autoSell = new PortfolioRequestDto();
//                     autoSell.setCompanyId(p.getCompanyId());
//                     autoSell.setQuantity(p.getQuantity());
//                     autoSell.setPrice(company.getCurrentPrice());
//                     autoSell.setPortfolioGroupId(p.getPortfolioGroupId());
//                     autoSell.setOrderType("SELL");
//                     this.sell(p.getUserId(), autoSell);
//                     System.out.println("AUTO-STOPLOSS: Sold " + p.getCompanyId() + " for User " + p.getUserId());
//                 }
//             } catch (Exception e) {
//                 System.err.println("Stop-loss check failed for " + p.getCompanyId() + ": " + e.getMessage());
//             }
//         }
//     }
//     @Override
//     public List<PortfolioResponseDto> getPortfolio(Long userId, Long portfolioId, String companyId) {
//         // ... (existing implementation is correct)
//         return portfolioRepository.findByUserIdAndPortfolioGroupId(userId, portfolioId)
//                 .stream().map(this::mapToDto).toList();
//     }
//     private PortfolioResponseDto mapToDto(Portfolio portfolio) {
//         return PortfolioResponseDto.builder()
//                 .companyId(portfolio.getCompanyId())
//                 .quantity(portfolio.getQuantity())
//                 .averageBuyPrice(portfolio.getAverageBuyPrice())
//                 .build();
//     }
// }
package com.example.portfolio_service.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfolio_service.client.CompanyClient;
import com.example.portfolio_service.client.ExchangeClient;
import com.example.portfolio_service.dto.CompanyResponseDto;
import com.example.portfolio_service.dto.OrderRequestDTO;
import com.example.portfolio_service.dto.OrderResponseDTO;
import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;
import com.example.portfolio_service.entity.Portfolio;
import com.example.portfolio_service.repository.PortfolioRepository;
import com.example.portfolio_service.service.IPortfolioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements IPortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ExchangeClient exchangeClient;
    private final CompanyClient companyClient;

    @Override
    @Transactional
    public PortfolioResponseDto buy(Long userId, PortfolioRequestDto request) {
        // 1. Prepare Order for Exchange including the user's intended price
        OrderRequestDTO order = OrderRequestDTO.builder()
                .userId(userId)
                .companyId(request.getCompanyId())
                .quantity(request.getQuantity())
                .price(request.getPrice()) // 🟢 Crucial: Passing the bid price
                .orderType("BUY")
                .build();

        // 2. Execute trade via Exchange Service
        OrderResponseDTO response = exchangeClient.placeOrder(order);

        // if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
        //     throw new RuntimeException("Order failed in exchange: " + (response != null ? response.getMessage() : "No response"));
        // }
        double executedPrice = response.getExecutedPrice();

        // 3. Update Portfolio: Handle "First-time buy" vs "Existing holding"
        Portfolio portfolio = portfolioRepository
                .findByUserIdAndPortfolioGroupIdAndCompanyId(
                        userId,
                        request.getPortfolioGroupId(),
                        request.getCompanyId())
                .orElse(Portfolio.builder()
                        .userId(userId)
                        .portfolioGroupId(request.getPortfolioGroupId())
                        .companyId(request.getCompanyId())
                        .quantity(0)
                        .averageBuyPrice(0.0)
                        .build());

        // Calculate New Weighted Average Price
        int oldQty = portfolio.getQuantity();
        double oldAvg = portfolio.getAverageBuyPrice();
        int newQty = oldQty + request.getQuantity();

        // Formula: ((Old Qty * Old Avg) + (New Qty * Executed Price)) / Total Qty
        double newAvg = ((oldQty * oldAvg) + (request.getQuantity() * executedPrice)) / newQty;

        portfolio.setQuantity(newQty);
        portfolio.setAverageBuyPrice(newAvg);

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
                .orElseThrow(() -> new RuntimeException("Stock not found in your portfolio"));

        if (portfolio.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient shares. Available: " + portfolio.getQuantity());
        }

        OrderRequestDTO order = OrderRequestDTO.builder()
                .userId(userId)
                .companyId(request.getCompanyId())
                .quantity(request.getQuantity())
                .price(request.getPrice()) // 🟢 Passing the sell price
                .orderType("SELL")
                .build();

        OrderResponseDTO response = exchangeClient.placeOrder(order);

        if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Sell order rejected by exchange");
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
        List<Portfolio> portfolios;
        if (companyId != null) {
            portfolios = portfolioRepository
                    .findByUserIdAndPortfolioGroupIdAndCompanyId(userId, portfolioId, companyId)
                    .map(List::of).orElse(List.of());
        } else {
            portfolios = portfolioRepository.findByUserIdAndPortfolioGroupId(userId, portfolioId);
        }
        return portfolios.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 5000)
    @Override
    public void checkAndExecuteStopLoss() {
        List<Portfolio> activeTriggers = portfolioRepository.findByStopLossPriceIsNotNull();
        for (Portfolio p : activeTriggers) {
            try {
                CompanyResponseDto company = companyClient.getCompanyData(p.getCompanyId());
                if (company.getCurrentPrice() <= p.getStopLossPrice()) {
                    // Logic to trigger automatic sell can be added here
                    System.out.println("Stop-loss triggered for " + p.getCompanyId());
                }
            } catch (Exception e) {
                System.err.println("Failed to check " + p.getCompanyId() + ": " + e.getMessage());
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
