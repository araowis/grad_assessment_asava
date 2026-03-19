package com.example.company_service.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.company_service.dto.CompanyDTO;
import com.example.company_service.models.Company;
import com.example.company_service.models.StockPriceHistory;
import com.example.company_service.repository.CompanyRepository;
import com.example.company_service.repository.HistoryRepository;
import com.example.company_service.service.ICompanyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;

@Service
public class CompanyService implements ICompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    @Autowired
    private CompanyRepository companyRepo;
    @Autowired
    private HistoryService historyService;

    @Override
    public Company addCompany(Company company) {

        // set opening = current at start
        company.setOpeningPrice(company.getCurrentPrice());

        // Check if already exists
        if (companyRepo.existsByShortId(company.getShortId())) {
            throw new RuntimeException("Company with this shortID already exists");
        }

        return companyRepo.save(company);
    }

    @Override
    public List<Company> getAllCompanies() {
        return companyRepo.findAll();
    }

    @Override
    public Company getCompanyById(String shortID) {
        if (!companyRepo.existsByShortId(shortID)) {
            throw new RuntimeException("Company not found");
        }
        return companyRepo.findByShortId(shortID);
    }

    @Override
    public Company updateCompany(String shortID, Company updated) {

        Company company = companyRepo.findByShortId(shortID);

        if (company == null) {
            throw new RuntimeException("Company not found");
        }

        company.setName(updated.getName());
        company.setNoOfShare(updated.getNoOfShare());
        company.setCurrentPrice(updated.getCurrentPrice());

        return companyRepo.save(company);
    }

    @Override
    public void deleteCompany(String shortID) {

        if (!companyRepo.existsByShortId(shortID)) {
            throw new RuntimeException("Company not found");
        }

        companyRepo.deleteByShortId(shortID);
    }

    @Override
    public boolean updatePrice(String id, double newPrice) {
        Company company = companyRepo.findById(id).orElseThrow(
                () -> new RuntimeException("Company not found"));

        double openingPrice = company.getOpeningPrice(); // reset daily
        double minPrice = openingPrice * 0.8; // -20%
        double maxPrice = openingPrice * 1.2; // +20%

        if (newPrice < minPrice || newPrice > maxPrice) {
            return false; // price deviation invalid
        }

        company.setCurrentPrice(newPrice);
        companyRepo.save(company);
        return true;
    }

    // 🔥 RUNS EVERY DAY AT 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void resetOpeningPrice() {

        List<Company> companies = companyRepo.findAll();

        for (Company company : companies) {
            company.setOpeningPrice(company.getCurrentPrice());
        }

        companyRepo.saveAll(companies);

        System.out.println("Opening prices reset for all companies");
    }

    @Transactional
    public void batchUpdatePrices(List<CompanyDTO> dtos) {
        List<String> ids = dtos.stream()
                .map(CompanyDTO::getShortId)
                .toList();

        log.info("Batch price update started for {} companies", ids.size());

        List<Company> companies = companyRepo.findByShortIdIn(ids);

        Map<String, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(Company::getShortId, c -> c));

        List<Company> updatedCompanies = new ArrayList<>();

        for (CompanyDTO dto : dtos) {
            Company company = companyMap.get(dto.getShortId());
            if (company == null) {
                log.warn("Company not found in DB for shortId: {}", dto.getShortId());
                continue;
            }

            double openingPrice = company.getOpeningPrice();
            double newPrice = dto.getCurrentPrice();

            if (newPrice >= openingPrice * 0.8 && newPrice <= openingPrice * 1.2) {
                log.debug("Updating {} | old: {} → new: {}", dto.getShortId(), company.getCurrentPrice(), newPrice);
                company.setCurrentPrice(newPrice);
                updatedCompanies.add(company);
                historyService.updateCompanyHistoryPrice(dto.getShortId(), newPrice, company);
            } else {
                log.warn("Price rejected for {} | new: {} outside circuit breaker band [{}, {}]",
                        dto.getShortId(), newPrice, openingPrice * 0.8, openingPrice * 1.2);
            }
        }

        companyRepo.saveAll(updatedCompanies);
        log.info("Batch price update completed — {} companies updated", updatedCompanies.size());
    }

}
