import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompanyService } from '../../services/company';
import { Company } from '../../models/company';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {

  companies: Company[] = [];
  stats: any[] = [];
  isLoading = true;

  constructor(private companyService: CompanyService) { }

  ngOnInit(): void {
    this.loadCompanies();
  }

  loadCompanies() {
    this.companyService.getAllCompanies().subscribe({
      next: (data) => {
        this.companies = data;
        this.calculateStats();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching companies', err);
        this.isLoading = false;
      }
    });
  }

  calculateStats() {
    if (!this.companies || this.companies.length === 0) {
      this.stats = [];
      return;
    }

    let totalValue = 0;
    let totalGain = 0;
    let topGainer: Company | undefined;
    let maxGainPercent = -Infinity;

    this.companies.forEach(c => {
      const value = c.currentPrice * c.noOfShare;
      totalValue += value;

      const gain = (c.currentPrice - c.openingPrice) * c.noOfShare;
      totalGain += gain;

      const gainPercent =
        c.openingPrice === 0
          ? 0
          : ((c.currentPrice - c.openingPrice) / c.openingPrice) * 100;

      if (gainPercent > maxGainPercent) {
        maxGainPercent = gainPercent;
        topGainer = c;
      }
    });

    this.stats = [
      {
        label: 'Total Value',
        value: `$${totalValue.toFixed(2)}`,
        sub: `${this.companies.length} companies`,
        icon: 'INR',
        color: 'text-blue-500'
      },
      {
        label: 'Total Gain',
        value: `$${totalGain.toFixed(2)}`,
        sub: `${((totalGain / totalValue) * 100 || 0).toFixed(2)}% return`,
        icon: '📈',
        color: 'text-emerald-500'
      },
      {
        label: 'Top Gainer',
        value: topGainer?.shortId || '-',
        sub: topGainer ? `${maxGainPercent.toFixed(2)}% today` : 'N/A',
        icon: '🔝',
        color: 'text-emerald-400'
      }
    ];
  }

  get marketData() {
    return this.companies.map(c => {
      const changePercent =
        ((c.currentPrice - c.openingPrice) / c.openingPrice) * 100;

      return {
        symbol: c.shortId,
        name: c.name,
        price: `$${c.currentPrice.toFixed(2)}`,
        change: `${changePercent >= 0 ? '+' : ''}${changePercent.toFixed(2)}%`,
        color: changePercent >= 0 ? 'bg-green-500' : 'bg-red-500'
      };
    });
  }
}