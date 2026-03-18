import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  // Toggle state: 'card' or 'list'
  marketViewMode: 'card' | 'list' = 'card';

  dashboardData: any = null;
  isLoading = false;
  error: string | null = null;

  stats = [
    { label: 'Total Value', value: '$41,431.50', sub: '3 assets', icon: '💲', color: 'text-blue-500' },
    { label: 'Total Gain', value: '$3,056.50', sub: '7.96% return', icon: '📈', color: 'text-emerald-500' },
    { label: 'Available Cash', value: '$25,000.00', sub: 'Ready to invest', icon: '🏦', color: 'text-blue-400' },
    { label: 'Top Gainer', value: 'SOL', sub: '+6.51% today', icon: '🔝', color: 'text-emerald-400' }
  ];

  marketData = [
    { symbol: 'BTC', name: 'Bitcoin', price: '$64,231.00', change: '+2.4%', color: 'bg-orange-500' },
    { symbol: 'ETH', name: 'Ethereum', price: '$3,452.12', change: '-1.1%', color: 'bg-blue-500' },
    { symbol: 'SOL', name: 'Solana', price: '$145.67', change: '+6.5%', color: 'bg-purple-500' }
  ];

  constructor(private http: HttpClient, private router: Router) {
    // Load saved preference from localStorage
    const savedMode = localStorage.getItem('marketViewMode') as 'card' | 'list' | null;
    if (savedMode) {
      this.marketViewMode = savedMode;
    }
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;

    this.http.get<any>('http://localhost:8081/api/dashboard').subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.applyDashboardData(data);
      },
      error: (err: any) => {
        console.error('Dashboard load failed', err);
        this.error = 'Unable to load dashboard data. Please try again later.';
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  private applyDashboardData(data: any): void {
    // Map API fields to UI fields; avoid breaking UI if fields are missing
    const totalValue = data?.totalValue ?? '$0.00';
    const totalGain = data?.totalGain ?? '$0.00';
    const availableCash = data?.availableCash ?? '$0.00';

    const assets = Array.isArray(data?.assets) ? data.assets : [];

    this.stats = [
      { label: 'Total Value', value: totalValue, sub: `${assets.length} assets`, icon: '💲', color: 'text-blue-500' },
      { label: 'Total Gain', value: totalGain, sub: `${data?.gainPercent ?? 0}% return`, icon: '📈', color: 'text-emerald-500' },
      { label: 'Available Cash', value: availableCash, sub: 'Ready to invest', icon: '🏦', color: 'text-blue-400' },
      { label: 'Top Gainer', value: data?.topGainer ?? '—', sub: data?.topGainerChange ? `${data.topGainerChange}` : '', icon: '🔝', color: 'text-emerald-400' }
    ];

    this.marketData = assets.map((item: any) => ({
      symbol: item.symbol ?? '---',
      name: item.name ?? 'Unknown',
      price: item.price ? `$${item.price}` : '$0.00',
      change: item.change ? `${item.change}` : '+0%',
      color: item.color ?? 'bg-slate-500',
    }));
  }

  goToCompany(symbol: string, name: string): void {
    // Use an absolute route so navigation works from anywhere under /app
    this.router.navigate(['/app/company-details', symbol], { queryParams: { name } });
  }

  /**
   * Toggle market view between card and list layouts
   */
  toggleMarketView(): void {
    this.marketViewMode = this.marketViewMode === 'card' ? 'list' : 'card';
    // Save toggle state to localStorage
    localStorage.setItem('marketViewMode', this.marketViewMode);
  }

  /**
   * Check if current view mode is card
   */
  isCardView(): boolean {
    return this.marketViewMode === 'card';
  }

  /**
   * Check if current view mode is list
   */
  isListView(): boolean {
    return this.marketViewMode === 'list';
  }
}
