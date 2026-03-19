package com.example.company_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.company_service.models.Company;

public interface CompanyRepository extends JpaRepository<Company, String> {

    boolean existsByShortId(String shortID);

    Company findByShortId(String shortID);

    void deleteByShortId(String shortID);

    List<Company> findByShortIdIn(List<String> ids);
}
