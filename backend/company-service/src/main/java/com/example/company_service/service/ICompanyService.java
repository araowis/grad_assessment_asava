package com.example.company_service.service;

import java.util.List;

import com.example.company_service.models.Company;

public interface ICompanyService {

    Company addCompany(Company company);

    List<Company> getAllCompanies();

    Company getCompanyById(String shortID);

    Company updateCompany(String shortID, Company company);

    void deleteCompany(String shortID);

    boolean updatePrice(String id, double newPrice);

}
