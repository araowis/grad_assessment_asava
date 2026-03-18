package com.example.company_service.service.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.company_service.models.Company;
import com.example.company_service.repository.CompanyRepository;
import com.example.company_service.service.ICompanyService;

@Service
public class CompanyService implements ICompanyService {

    @Autowired
    private CompanyRepository repo;

    @Override
    public Company addCompany(Company company) {

        // set opening = current at start
        company.setOpeningPrice(company.getCurrentPrice());

        // Check if already exists
        if (repo.existsByShortId(company.getShortId())) {
            throw new RuntimeException("Company with this shortID already exists");
        }

        return repo.save(company);
    }

    @Override
    public List<Company> getAllCompanies() {
        return repo.findAll();
    }

    @Override
    public Company getCompanyById(String shortID) {
        if (!repo.existsByShortId(shortID)) {
            throw new RuntimeException("Company not found");
        }
        return repo.findByShortId(shortID);
    }

    @Override
    public Company updateCompany(String shortID, Company updated) {

        Company company = repo.findByShortId(shortID);

        if (company == null) {
            throw new RuntimeException("Company not found");
        }

        company.setName(updated.getName());
        company.setNoOfShare(updated.getNoOfShare());
        company.setCurrentPrice(updated.getCurrentPrice());

        return repo.save(company);
    }

    @Override
    public void deleteCompany(String shortID) {

        if (!repo.existsByShortId(shortID)) {
            throw new RuntimeException("Company not found");
        }

        repo.deleteByShortId(shortID);
    }

    @Override
    public boolean updatePrice(String id, double newPrice) {
        Company company = repo.findById(id).orElseThrow(
                () -> new RuntimeException("Company not found")
        );

        double openingPrice = company.getOpeningPrice(); // reset daily
        double minPrice = openingPrice * 0.8;  // -20%
        double maxPrice = openingPrice * 1.2;  // +20%

        if (newPrice < minPrice || newPrice > maxPrice) {
            return false; // price deviation invalid
        }

        company.setCurrentPrice(newPrice);
        repo.save(company);
        return true;
    }

    // 🔥 RUNS EVERY DAY AT 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void resetOpeningPrice() {

        List<Company> companies = repo.findAll();

        for (Company company : companies) {
            company.setOpeningPrice(company.getCurrentPrice());
        }

        repo.saveAll(companies);

        System.out.println("Opening prices reset for all companies");
    }

}
