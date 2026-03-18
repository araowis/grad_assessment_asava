import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService, AdminStats } from '../../../services/admin';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit {
  stats: AdminStats = {
    totalCompanies: 0,
    totalStocksListed: 0,
    totalMarketCap: 0,
    totalTrades: 0,
    recentCompanies: [],
  };
  loading = true;

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.adminService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: () => {
        // ── Dev fallback: remove once backend endpoint is ready ──
        this.stats = {
          totalCompanies: 8,
          totalStocksListed: 3200000,
          totalMarketCap: 74500000,
          totalTrades: 1842,
          recentCompanies: [
            { id: 1, name: 'Infosys Ltd',       stockSymbol: 'INFY', totalStocks: 420000000, currentPrice: 1482.50 },
            { id: 2, name: 'Tata Consultancy',  stockSymbol: 'TCS',  totalStocks: 365000000, currentPrice: 3748.00 },
            { id: 3, name: 'Reliance Ind.',      stockSymbol: 'RIL',  totalStocks: 676000000, currentPrice: 2820.30 },
            { id: 4, name: 'HDFC Bank',          stockSymbol: 'HDFC', totalStocks: 556000000, currentPrice: 1647.80 },
          ],
        };
        this.loading = false;
      },
    });
  }
}