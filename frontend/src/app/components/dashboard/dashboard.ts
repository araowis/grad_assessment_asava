import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompanyService } from '../../services/company';
import { Auth } from '../../services/auth';
import { Company } from '../../models/company';
import { Trading } from '../trading/trading';
import { FormsModule } from '@angular/forms';
import { PortfolioStateService } from '../../services/portfolio-state.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, Trading, FormsModule],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  companies: Company[] = [];
  isLoading = true;
  formattedMarketData: any[] = [];

  // State for the Trading Component
  selectedCompany: any = null;
  selectedSymbol: string | null = null;
  selectedPrice: number = 0;
  currentUserId: number = 0;
  activePortfolioId = 1;

  stats: any[] = [];

  constructor(
    private companyService: CompanyService,
    private authService: Auth,
    private cdr: ChangeDetectorRef,
    private portfolioState: PortfolioStateService,
  ) {}

  ngOnInit(): void {
    this.loadCompanies();
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id ? Number(user.id) : 0;
    this.portfolioState.selectedId$.subscribe((id) => {
      this.activePortfolioId = id;
    });
  }

  loadCompanies() {
    this.isLoading = true;
    this.companyService.getAllCompanies().subscribe({
      next: (data: any) => {
        this.companies = Array.isArray(data) ? data : [];
        this.calculateStats();
        this.processMarketData();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  selectCompany(item: any) {
    // Show loading while fetching specific company details
    this.isLoading = true;

    // 🟢 Fetching fresh data from backend to ensure currentPrice is accurate
    this.companyService.getCompanyById(item.symbol).subscribe({
      next: (company: Company) => {
        this.selectedSymbol = company.shortId;
        this.selectedPrice = company.currentPrice;

        this.selectedCompany = {
          symbol: company.shortId,
          name: company.name,
          price: company.currentPrice,
        };

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Could not fetch company details', err);
        this.isLoading = false;
        alert('Failed to load real-time data for this company.');
        this.cdr.detectChanges();
      },
    });
  }

  clearSelection() {
    this.selectedCompany = null;
    this.selectedSymbol = null;
    this.selectedPrice = 0;
    this.cdr.detectChanges();
  }

  processMarketData() {
    this.formattedMarketData = this.companies.map((c) => {
      const opening = c.openingPrice || c.currentPrice;
      const changePercent = ((c.currentPrice - opening) / opening) * 100;
      return {
        symbol: c.shortId,
        name: c.name,
        price: `₹${c.currentPrice.toFixed(2)}`,
        change: `${changePercent >= 0 ? '+' : ''}${changePercent.toFixed(2)}%`,
        color: changePercent >= 0 ? 'text-emerald-400' : 'text-rose-400',
        bg: changePercent >= 0 ? 'bg-emerald-500/10' : 'bg-rose-500/10',
      };
    });
  }

  calculateStats() {
    if (!this.companies.length) return;
    const totalCap = this.companies.reduce(
      (acc, c) => acc + c.currentPrice * (c.noOfShare || 0),
      0,
    );
    this.stats = [
      {
        label: 'Market Cap',
        value: `₹${(totalCap / 10000000).toFixed(2)}Cr`,
        icon: '💎',
        color: 'text-blue-500',
        sub: 'Total Valuation',
      },
      {
        label: 'Active Assets',
        value: this.companies.length,
        icon: '🏢',
        color: 'text-emerald-500',
        sub: 'Listed Companies',
      },
      {
        label: '24h Volume',
        value: '₹12.4L',
        icon: '📊',
        color: 'text-amber-500',
        sub: '+5.2% from yesterday',
      },
    ];
  }
}
