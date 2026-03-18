package com.example.company_service.controller;

import com.example.company_service.dto.CompanyDTO;
import com.example.company_service.models.Company;
import com.example.company_service.service.ICompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICompanyService service;

    @Autowired
    private ObjectMapper objectMapper;

    private Company sampleCompany;
    private CompanyDTO sampleDTO;

    // ─── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("TCS", "Tata Consultancy Services", 1000, 100.0, 105.0);
        sampleDTO     = new CompanyDTO("TCS", "Tata Consultancy Services", 1000, 100.0, 105.0);
    }

    // ─── POST /api/v1/companies ────────────────────────────────────────────────

    @Test
    void addCompany_ShouldReturn201_WhenSuccessful() throws Exception {
        when(service.addCompany(any(Company.class))).thenReturn(sampleCompany);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortId").value("TCS"))
                .andExpect(jsonPath("$.name").value("Tata Consultancy Services"));
    }

    @Test
    void addCompany_ShouldReturn400_WhenDuplicateShortId() throws Exception {
        when(service.addCompany(any(Company.class)))
                .thenThrow(new RuntimeException("Company with this shortID already exists"));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Company with this shortID already exists"));
    }

    // ─── GET /api/v1/companies ─────────────────────────────────────────────────

    @Test
    void getAllCompanies_ShouldReturn200_WithListOfCompanies() throws Exception {
        Company c2 = new Company("INFY", "Infosys", 500, 200.0, 210.0);
        when(service.getAllCompanies()).thenReturn(Arrays.asList(sampleCompany, c2));

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shortId").value("TCS"))
                .andExpect(jsonPath("$[1].shortId").value("INFY"));
    }

    @Test
    void getAllCompanies_ShouldReturn200_WithEmptyList() throws Exception {
        when(service.getAllCompanies()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── GET /api/v1/companies/{shortId} ──────────────────────────────────────

    @Test
    void getCompany_ShouldReturn200_WhenCompanyFound() throws Exception {
        when(service.getCompanyById("TCS")).thenReturn(sampleCompany);

        mockMvc.perform(get("/api/v1/companies/TCS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortId").value("TCS"))
                .andExpect(jsonPath("$.name").value("Tata Consultancy Services"));
    }

    @Test
    void getCompany_ShouldReturn404_WhenNotFound() throws Exception {
        when(service.getCompanyById("UNKNOWN"))
                .thenThrow(new RuntimeException("Company not found"));

        mockMvc.perform(get("/api/v1/companies/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Company not found"));
    }

    // ─── PUT /api/v1/companies/{shortId} ──────────────────────────────────────

    @Test
    void updateCompany_ShouldReturn200_WhenSuccessful() throws Exception {
        Company updated = new Company("TCS", "TCS Updated", 2000, 100.0, 110.0);
        when(service.updateCompany(eq("TCS"), any(Company.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/companies/TCS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TCS Updated"))
                .andExpect(jsonPath("$.noOfShare").value(2000));
    }

    @Test
    void updateCompany_ShouldReturn404_WhenNotFound() throws Exception {
        when(service.updateCompany(eq("UNKNOWN"), any(Company.class)))
                .thenThrow(new RuntimeException("Company not found"));

        mockMvc.perform(put("/api/v1/companies/UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Company not found"));
    }

    // ─── DELETE /api/v1/companies/{shortId} ───────────────────────────────────

    @Test
    void deleteCompany_ShouldReturn200_WhenSuccessful() throws Exception {
        doNothing().when(service).deleteCompany("TCS");

        mockMvc.perform(delete("/api/v1/companies/TCS"))
                .andExpect(status().isOk())
                .andExpect(content().string("Company deleted successfully"));
    }

    @Test
    void deleteCompany_ShouldReturn404_WhenNotFound() throws Exception {
        doThrow(new RuntimeException("Company not found")).when(service).deleteCompany("UNKNOWN");

        mockMvc.perform(delete("/api/v1/companies/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Company not found"));
    }

    // ─── PUT /api/v1/companies/{id}/price ──────────────────────────────────────

    @Test
    void updatePrice_ShouldReturn200_WhenPriceIsValid() throws Exception {
        when(service.updatePrice("TCS", 110.0)).thenReturn(true);

        mockMvc.perform(put("/api/v1/companies/TCS/price")
                        .param("price", "110.0"))
                .andExpect(status().isOk())
                .andExpect(content().string("Price updated successfully"));
    }

    @Test
    void updatePrice_ShouldReturn400_WhenPriceExceedsDeviation() throws Exception {
        when(service.updatePrice("TCS", 999.0)).thenReturn(false);

        mockMvc.perform(put("/api/v1/companies/TCS/price")
                        .param("price", "999.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Price update failed: exceeds allowed deviation limits"));
    }
}