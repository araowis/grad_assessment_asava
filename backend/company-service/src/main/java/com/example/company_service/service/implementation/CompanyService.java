package com.example.company_service.service.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
        company.setPrice(updated.getPrice());

        return repo.save(company);
    }

    @Override
    public void deleteCompany(String shortID) {

        if (!repo.existsByShortId(shortID)) {
            throw new RuntimeException("Company not found");
        }

        repo.deleteByShortId(shortID);
    }
}
