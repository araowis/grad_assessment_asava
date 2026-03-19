// import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
// import { PortfolioService, PortfolioResponse } from '../../services/portfolio';
// import { CompanyService } from '../../services/company';
// import { Company } from '../../models/company';
// import { ExchangeService } from '../../services/exchange';
// import { PortfolioStateService } from '../../services/profile-state.service.ts';
// import { Trading } from '../trading/trading'; // 🟢 Import Trading Component
// import { CommonModule } from '@angular/common';
// import { FormsModule } from '@angular/forms';

// @Component({
//   selector: 'app-wallet',
//   templateUrl: './wallet.html',
//   standalone: true,
//   imports: [CommonModule, FormsModule, Trading], // 🟢 Add Trading to imports
// })
// export class Wallet implements OnInit {
//   userId = 2;
//   selectedPortfolioId = 1;
//   portfolioIds: number[] = [1];

//   // 🟢 State for selection
//   selectedStock: any = null;

//   holdings: any[] = [];
//   transactions: any[] = [];
//   isLoading = true;
//   totalBalance = 0;
//   totalPnL = 0;

//   constructor(
//     public portfolioService: PortfolioService,
//     private companyService: CompanyService,
//     private exchangeService: ExchangeService,
//     private portfolioState: PortfolioStateService,
//     private cdr: ChangeDetectorRef,
//   ) {}

//   ngOnInit() {
//     this.portfolioState.selectedId$.subscribe((id) => {
//       this.selectedPortfolioId = id;
//       this.loadData();
//     });
//   }

//   // 🟢 Click handler for table rows
//   selectHolding(stock: any) {
//     this.selectedStock = {
//       symbol: stock.companyId,
//       price: stock.currentPrice,
//     };
//     this.cdr.detectChanges();
//   }

//   loadData() {
//     this.isLoading = true;
//     this.cdr.detectChanges();

//     this.portfolioService.getPortfolio(this.userId, this.selectedPortfolioId).subscribe({
//       next: (holdings: PortfolioResponse[]) => {
//         this.companyService.getAllCompanies().subscribe((companies: Company[]) => {
//           this.holdings = holdings.map((h) => {
//             const marketData = companies.find((c) => c.shortId === h.companyId);
//             const currentPrice = marketData ? marketData.currentPrice : h.averageBuyPrice;
//             return {
//               ...h,
//               currentPrice,
//               marketValue: currentPrice * h.quantity,
//               pnl: (currentPrice - h.averageBuyPrice) * h.quantity,
//               pnlPercent:
//                 h.averageBuyPrice > 0
//                   ? ((currentPrice - h.averageBuyPrice) / h.averageBuyPrice) * 100
//                   : 0,
//             };
//           });
//           this.calculateStats();
//           this.isLoading = false;
//           this.cdr.detectChanges();
//         });
//       },
//     });

//     this.exchangeService.getUserOrders(this.userId).subscribe((orders) => {
//       this.transactions = orders
//         .map((o) => ({
//           symbol: o.companyId,
//           type: o.type,
//           qty: o.quantity,
//           price: o.price,
//           date: o.createdAt ? new Date(o.createdAt) : new Date(),
//           status: o.status,
//         }))
//         .reverse();
//       this.cdr.detectChanges();
//     });
//   }

//   changePortfolio(id: number) {
//     this.portfolioState.setPortfolio(id);
//     this.selectedStock = null; // Clear trading view on switch
//   }

//   addNewPortfolio() {
//     const nextId = Math.max(...this.portfolioIds) + 1;
//     this.portfolioIds.push(nextId);
//     this.portfolioState.setPortfolio(nextId);
//   }

//   calculateStats() {
//     this.totalBalance = this.holdings.reduce((acc, curr) => acc + (curr.marketValue || 0), 0);
//     this.totalPnL = this.holdings.reduce((acc, curr) => acc + (curr.pnl || 0), 0);
//   }
// }

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortfolioService, PortfolioResponse } from '../../services/portfolio';
import { CompanyService } from '../../services/company';
import { ExchangeService } from '../../services/exchange';
import { PortfolioStateService } from '../../services/portfolio-state.service';
import { Trading } from '../trading/trading';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.html',
  standalone: true,
  imports: [CommonModule, FormsModule, Trading],
})
export class Wallet implements OnInit {
  userId = 2; // Dynamic in real auth
  selectedPortfolioId = 1;
  portfolioIds: number[] = [1];

  holdings: any[] = [];
  transactions: any[] = [];
  selectedStock: any = null;

  totalBalance = 0;
  totalPnL = 0;
  isLoading = true;

  constructor(
    private portfolioService: PortfolioService,
    private companyService: CompanyService,
    private exchangeService: ExchangeService,
    private portfolioState: PortfolioStateService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    // Sync with global portfolio selection
    this.portfolioState.selectedId$.subscribe((id) => {
      this.selectedPortfolioId = id;
      this.loadData();
    });
  }

  loadData() {
    this.isLoading = true;
    this.cdr.detectChanges();

    // 1. Fetch Holdings & calculate Real-time value
    this.portfolioService.getPortfolio(this.userId, this.selectedPortfolioId).subscribe({
      next: (res: PortfolioResponse[]) => {
        this.companyService.getAllCompanies().subscribe((companies) => {
          this.holdings = res.map((h) => {
            const market = companies.find((c) => c.shortId === h.companyId);
            const currentPrice = market ? market.currentPrice : h.averageBuyPrice;
            return {
              ...h,
              currentPrice,
              marketValue: currentPrice * h.quantity,
              pnl: (currentPrice - h.averageBuyPrice) * h.quantity,
              pnlPercent:
                h.averageBuyPrice > 0
                  ? ((currentPrice - h.averageBuyPrice) / h.averageBuyPrice) * 100
                  : 0,
            };
          });
          this.calculateStats();
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => (this.isLoading = false),
    });

    // 2. Fetch Full Transaction History
    this.exchangeService.getUserOrders(this.userId).subscribe((orders) => {
      this.transactions = orders
        .map((o) => ({
          ...o,
          date: o.createdAt || new Date(),
        }))
        .reverse();
      this.cdr.detectChanges();
    });
  }

  calculateStats() {
    this.totalBalance = this.holdings.reduce((acc, curr) => acc + curr.marketValue, 0);
    this.totalPnL = this.holdings.reduce((acc, curr) => acc + curr.pnl, 0);
  }

  selectHolding(stock: any) {
    this.selectedStock = {
      symbol: stock.companyId,
      price: stock.currentPrice,
    };
    this.cdr.detectChanges();
  }

  addNewPortfolio() {
    const nextId = Math.max(...this.portfolioIds) + 1;
    this.portfolioIds.push(nextId);
    this.changePortfolio(nextId);
  }

  changePortfolio(id: number) {
    this.portfolioState.setPortfolio(id);
    this.selectedStock = null;
  }
}
