package com.example.company_service.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.company_service.dto.CompanyDTO;
import com.example.company_service.models.Company;
import com.example.company_service.service.ICompanyService;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    @Autowired
    private ICompanyService service;

    // 🔄 DTO → Entity
    private Company mapToEntity(CompanyDTO dto) {
        Company c = new Company();
        c.setShortId(dto.getShortId());
        c.setName(dto.getName());
        c.setNoOfShare(dto.getNoOfShare());
        c.setCurrentPrice(dto.getCurrentPrice());
        c.setOpeningPrice(dto.getOpeningPrice());
        return c;
    }

    // 🔄 Entity → DTO
    private CompanyDTO mapToDTO(Company company) {
        return new CompanyDTO(
                company.getShortId(),
                company.getName(),
                company.getNoOfShare(),
                company.getOpeningPrice(),
                company.getCurrentPrice()
        );
    }

    // ➕Add Company
    @PostMapping
    public ResponseEntity<?> addCompany(@RequestBody CompanyDTO dto) {
        try {
            Company saved = service.addCompany(mapToEntity(dto));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // 📋 Get All Companies
    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {

        List<CompanyDTO> list = service.getAllCompanies()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    // 🔍 Get by shortId
    @GetMapping("/{shortId}")
    public ResponseEntity<?> getCompany(@PathVariable String shortId) {
        try {
            Company company = service.getCompanyById(shortId);
            return ResponseEntity.ok(mapToDTO(company));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // ✏️ Update
    @PutMapping("/{shortId}")
    public ResponseEntity<?> updateCompany(
            @PathVariable String shortId,
            @RequestBody CompanyDTO dto) {

        try {
            Company updated = service.updateCompany(shortId, mapToEntity(dto));
            return ResponseEntity.ok(mapToDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // ❌ Delete
    @DeleteMapping("/{shortId}")
    public ResponseEntity<?> deleteCompany(@PathVariable String shortId) {
        try {
            service.deleteCompany(shortId);
            return ResponseEntity.ok("Company deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<String> updatePrice(
            @PathVariable String id,
            @RequestParam double price) {

        boolean updated = service.updatePrice(id, price);

        if (updated) {
            return ResponseEntity.ok("Price updated successfully");
        } else {
            return ResponseEntity.badRequest()
                    .body("Price update failed: exceeds allowed deviation limits");
        }
    }
}
