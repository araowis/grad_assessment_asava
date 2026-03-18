package com.example.company_service.service.implementation;

import com.example.company_service.models.Company;
import com.example.company_service.repository.CompanyRepository;
import com.example.company_service.service.implementation.CompanyService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository repo;

    @InjectMocks
    private CompanyService companyService;

    private Company sampleCompany;

    // ─── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("TCS", "Tata Consultancy Services", 1000, 100.0, 100.0);
    }

    // ─── addCompany ───────────────────────────────────────────────────────────

    @Test
    void addCompany_ShouldSaveAndReturn_WhenShortIdIsUnique() {
        when(repo.existsByShortId("TCS")).thenReturn(false);
        when(repo.save(any(Company.class))).thenReturn(sampleCompany);

        Company result = companyService.addCompany(sampleCompany);

        assertNotNull(result);
        assertEquals("TCS", result.getShortId());
        // opening price should be set equal to current price on add
        assertEquals(sampleCompany.getCurrentPrice(), result.getOpeningPrice());
        verify(repo, times(1)).save(sampleCompany);
    }

    @Test
    void addCompany_ShouldThrowException_WhenShortIdAlreadyExists() {
        when(repo.existsByShortId("TCS")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyService.addCompany(sampleCompany));

        assertEquals("Company with this shortID already exists", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void addCompany_ShouldSetOpeningPriceToCurrentPrice() {
        Company company = new Company("INFY", "Infosys", 500, 0.0, 250.0);
        when(repo.existsByShortId("INFY")).thenReturn(false);
        when(repo.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company result = companyService.addCompany(company);

        // Opening price must be synced to current price at creation
        assertEquals(250.0, result.getOpeningPrice());
    }

    // ─── getAllCompanies ───────────────────────────────────────────────────────

    @Test
    void getAllCompanies_ShouldReturnAllCompanies() {
        Company c2 = new Company("INFY", "Infosys", 500, 200.0, 200.0);
        when(repo.findAll()).thenReturn(Arrays.asList(sampleCompany, c2));

        List<Company> result = companyService.getAllCompanies();

        assertEquals(2, result.size());
        verify(repo, times(1)).findAll();
    }

    @Test
    void getAllCompanies_ShouldReturnEmptyList_WhenNoCompaniesExist() {
        when(repo.findAll()).thenReturn(List.of());

        List<Company> result = companyService.getAllCompanies();

        assertTrue(result.isEmpty());
    }

    // ─── getCompanyById ────────────────────────────────────────────────────────

    @Test
    void getCompanyById_ShouldReturnCompany_WhenExists() {
        when(repo.existsByShortId("TCS")).thenReturn(true);
        when(repo.findByShortId("TCS")).thenReturn(sampleCompany);

        Company result = companyService.getCompanyById("TCS");

        assertNotNull(result);
        assertEquals("TCS", result.getShortId());
    }

    @Test
    void getCompanyById_ShouldThrowException_WhenNotFound() {
        when(repo.existsByShortId("UNKNOWN")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyService.getCompanyById("UNKNOWN"));

        assertEquals("Company not found", ex.getMessage());
    }

    // ─── updateCompany ─────────────────────────────────────────────────────────

    @Test
    void updateCompany_ShouldUpdateAndReturn_WhenCompanyExists() {
        Company updatedData = new Company("TCS", "TCS Updated", 2000, 0.0, 150.0);
        when(repo.findByShortId("TCS")).thenReturn(sampleCompany);
        when(repo.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company result = companyService.updateCompany("TCS", updatedData);

        assertEquals("TCS Updated", result.getName());
        assertEquals(2000, result.getNoOfShare());
        assertEquals(150.0, result.getCurrentPrice());
        verify(repo, times(1)).save(sampleCompany);
    }

    @Test
    void updateCompany_ShouldThrowException_WhenCompanyNotFound() {
        when(repo.findByShortId("UNKNOWN")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyService.updateCompany("UNKNOWN", sampleCompany));

        assertEquals("Company not found", ex.getMessage());
        verify(repo, never()).save(any());
    }

    // ─── deleteCompany ─────────────────────────────────────────────────────────

    @Test
    void deleteCompany_ShouldDelete_WhenCompanyExists() {
        when(repo.existsByShortId("TCS")).thenReturn(true);
        doNothing().when(repo).deleteByShortId("TCS");

        assertDoesNotThrow(() -> companyService.deleteCompany("TCS"));

        verify(repo, times(1)).deleteByShortId("TCS");
    }

    @Test
    void deleteCompany_ShouldThrowException_WhenCompanyNotFound() {
        when(repo.existsByShortId("UNKNOWN")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyService.deleteCompany("UNKNOWN"));

        assertEquals("Company not found", ex.getMessage());
        verify(repo, never()).deleteByShortId(any());
    }

    // ─── updatePrice ───────────────────────────────────────────────────────────

    @Test
    void updatePrice_ShouldReturnTrue_WhenPriceIsWithinBounds() {
        // Opening price = 100.0 → allowed range [80.0, 120.0]
        Company company = new Company("TCS", "TCS", 1000, 100.0, 100.0);
        when(repo.findById("TCS")).thenReturn(Optional.of(company));
        when(repo.save(any(Company.class))).thenReturn(company);

        boolean result = companyService.updatePrice("TCS", 115.0);

        assertTrue(result);
        assertEquals(115.0, company.getCurrentPrice());
    }

    @Test
    void updatePrice_ShouldReturnFalse_WhenPriceExceedsUpperLimit() {
        // Opening price = 100.0 → max allowed = 120.0
        Company company = new Company("TCS", "TCS", 1000, 100.0, 100.0);
        when(repo.findById("TCS")).thenReturn(Optional.of(company));

        boolean result = companyService.updatePrice("TCS", 125.0);

        assertFalse(result);
        verify(repo, never()).save(any()); // price not saved
    }

    @Test
    void updatePrice_ShouldReturnFalse_WhenPriceBelowLowerLimit() {
        // Opening price = 100.0 → min allowed = 80.0
        Company company = new Company("TCS", "TCS", 1000, 100.0, 100.0);
        when(repo.findById("TCS")).thenReturn(Optional.of(company));

        boolean result = companyService.updatePrice("TCS", 75.0);

        assertFalse(result);
        verify(repo, never()).save(any());
    }

    @Test
    void updatePrice_ShouldReturnTrue_WhenPriceIsExactlyAtBoundary() {
        // Boundary values: exactly 80.0 and 120.0 should be valid
        Company company = new Company("TCS", "TCS", 1000, 100.0, 100.0);
        when(repo.findById("TCS")).thenReturn(Optional.of(company));
        when(repo.save(any())).thenReturn(company);

        assertTrue(companyService.updatePrice("TCS", 80.0));
        assertTrue(companyService.updatePrice("TCS", 120.0));
    }

    @Test
    void updatePrice_ShouldThrowException_WhenCompanyNotFound() {
        when(repo.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> companyService.updatePrice("UNKNOWN", 100.0));
    }

    // ─── resetOpeningPrice ─────────────────────────────────────────────────────

    @Test
    void resetOpeningPrice_ShouldSetOpeningPriceToCurrentPrice_ForAllCompanies() {
        Company c1 = new Company("TCS", "TCS", 1000, 100.0, 115.0);
        Company c2 = new Company("INFY", "Infosys", 500, 200.0, 195.0);
        when(repo.findAll()).thenReturn(Arrays.asList(c1, c2));

        companyService.resetOpeningPrice();

        // After reset, opening price = current price
        assertEquals(115.0, c1.getOpeningPrice());
        assertEquals(195.0, c2.getOpeningPrice());
        verify(repo, times(1)).saveAll(anyList());
    }
}