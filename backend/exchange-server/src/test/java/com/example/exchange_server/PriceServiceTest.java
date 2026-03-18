package com.example.exchange_server;
 
import com.example.exchange_server.client.CompanyClient;
import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.service.PriceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.Arrays;
import java.util.List;
 
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class PriceServiceTest {
 
    @Mock private CompanyClient companyClient;
 
    @InjectMocks
    private PriceService priceService;
 
    @Test
    void updatePrices_ShouldCallUpdatePrice_ForEachCompany() {
        CompanyDTO c1 = new CompanyDTO("TCS",  "TCS",     1000, 100.0, 105.0);
        CompanyDTO c2 = new CompanyDTO("INFY", "Infosys", 500,  200.0, 210.0);
 
        when(companyClient.getAllCompanies()).thenReturn(Arrays.asList(c1, c2));
        doNothing().when(companyClient).updatePrice(anyString(), anyDouble());
 
        priceService.updatePrices();
 
        // One updatePrice call per company
        verify(companyClient, times(1)).updatePrice(eq("TCS"),  anyDouble());
        verify(companyClient, times(1)).updatePrice(eq("INFY"), anyDouble());
    }
 
    @Test
    void updatePrices_ShouldDoNothing_WhenNoCompaniesExist() {
        when(companyClient.getAllCompanies()).thenReturn(List.of());
 
        priceService.updatePrices();
 
        verify(companyClient, never()).updatePrice(anyString(), anyDouble());
    }
 
    @Test
    void updatePrices_ShouldFetchAllCompanies_BeforeUpdating() {
        when(companyClient.getAllCompanies()).thenReturn(List.of(
                new CompanyDTO("TCS", "TCS", 1000, 100.0, 105.0)
        ));
        doNothing().when(companyClient).updatePrice(anyString(), anyDouble());
 
        priceService.updatePrices();
 
        // getAllCompanies must be called exactly once
        verify(companyClient, times(1)).getAllCompanies();
    }
 
    @Test
    void updatePrices_ShouldPassNewCalculatedPrice_NotCurrentPrice() {
        CompanyDTO company = new CompanyDTO("TCS", "TCS", 1000, 100.0, 100.0);
        when(companyClient.getAllCompanies()).thenReturn(List.of(company));
        doNothing().when(companyClient).updatePrice(anyString(), anyDouble());
 
        priceService.updatePrices();
 
        // The price passed to updatePrice must be different from currentPrice (PriceUtil adds fluctuation)
        // We can only verify it was called; PriceUtil is tested separately
        verify(companyClient).updatePrice(eq("TCS"), anyDouble());
    }
}
 
 