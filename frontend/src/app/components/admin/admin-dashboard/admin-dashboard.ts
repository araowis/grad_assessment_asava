import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService, AdminStats, Company } from '../../../services/admin';

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

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.loadDataAndCalculateStats();
  }

  loadDataAndCalculateStats() {
    this.loading = true;
    this.adminService.getCompanies().subscribe({
      next: (data: any[]) => {
        if (data && data.length > 0) {
          // 1. Map the raw data to our Company interface first
          // This ensures noOfShare (backend) -> totalStocks (frontend)
          const mappedCompanies: Company[] = data.map((c) => ({
            id: c.shortId,
            name: c.name,
            stockSymbol: c.shortId,
            totalStocks: c.noOfShare || 0,
            currentPrice: c.currentPrice || 0,
          }));

          // 2. Calculate aggregate values using the mapped data
          const totalStocks = mappedCompanies.reduce((acc, c) => acc + c.totalStocks, 0);
          const marketCap = mappedCompanies.reduce(
            (acc, c) => acc + c.totalStocks * c.currentPrice,
            0,
          );

          // 3. Update the stats object
          this.stats = {
            totalCompanies: mappedCompanies.length,
            totalStocksListed: totalStocks,
            totalMarketCap: marketCap,
            totalTrades: 1250, // Static placeholder or keep at 0
            recentCompanies: mappedCompanies.slice(-5).reverse(), // Last 5 companies
          };
        }

        this.loading = false;
        this.cdr.detectChanges(); // Force UI update
      },
      error: (err) => {
        console.error('Error fetching companies for stats:', err);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
