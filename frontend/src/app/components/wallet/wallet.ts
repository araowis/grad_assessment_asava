// import { Component, OnInit, ChangeDetectorRef } from '@angular/core'; // 🟢 Added ChangeDetectorRef
// import { PortfolioService, PortfolioResponse } from '../../services/portfolio';
// import { CompanyService } from '../../services/company';
// import { Company } from '../../models/company';
// import { FormsModule } from '@angular/forms';
// import { CommonModule } from '@angular/common';
// import { ExchangeService } from '../../services/exchange';
// import { PortfolioStateService } from '../../services/profile-state.service.ts';

// @Component({
//   selector: 'app-wallet',
//   templateUrl: './wallet.html',
//   styleUrls: ['./wallet.css'],
//   standalone: true,
//   imports: [CommonModule, FormsModule],
// })
// export class Wallet implements OnInit {
//   userId = 2;
//   selectedPortfolioId = 1;
//   portfolioIds: number[] = [1];

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
//     private cdr: ChangeDetectorRef, // 🟢 Injecting CDR
//   ) {}

//   ngOnInit() {
//     this.loadData();
//   }

//   addNewPortfolio() {
//     const nextId = this.portfolioIds.length > 0 ? Math.max(...this.portfolioIds) + 1 : 1;
//     this.portfolioIds.push(nextId);
//     this.changePortfolio(nextId);
//     this.portfolioState.setPortfolio(nextId);
//     // CDR call isn't strictly needed here as push() is tracked,
//     // but changePortfolio triggers loadData which has it.
//   }

//   changePortfolio(id: number) {
//     this.selectedPortfolioId = id;
//     this.loadData();
//   }

//   loadData() {
//     this.isLoading = true;
//     this.totalPnL = 0;
//     this.holdings = [];
//     this.transactions = [];
//     this.cdr.detectChanges(); // 🟢 Show loading state immediately

//     // 1. Get Real Portfolio Holdings
//     this.portfolioService.getPortfolio(this.userId, this.selectedPortfolioId).subscribe({
//       next: (holdings: PortfolioResponse[]) => {
//         if (holdings.length === 0) {
//           this.isLoading = false;
//           this.cdr.detectChanges(); // 🟢 Update UI for empty state
//           return;
//         }

//         // 2. Cross-reference with Company Service for real-time prices
//         this.companyService.getAllCompanies().subscribe((companies: Company[]) => {
//           this.holdings = holdings.map((h) => {
//             const marketData = companies.find((c) => c.shortId === h.companyId);
//             const currentPrice = marketData ? marketData.currentPrice : h.averageBuyPrice;

//             const marketValue = currentPrice * h.quantity;
//             const pnl = (currentPrice - h.averageBuyPrice) * h.quantity;
//             const pnlPercent =
//               h.averageBuyPrice > 0
//                 ? ((currentPrice - h.averageBuyPrice) / h.averageBuyPrice) * 100
//                 : 0;

//             return {
//               ...h,
//               currentPrice: currentPrice,
//               marketValue: marketValue,
//               pnl: pnl,
//               pnlPercent: pnlPercent,
//             };
//           });

//           this.calculateStats();
//           this.isLoading = false;
//           this.cdr.detectChanges(); // 🟢 CRITICAL: Notify Angular that holdings and stats are ready
//         });
//       },
//       error: (err) => {
//         console.error('Portfolio Load Error:', err);
//         this.isLoading = false;
//         this.cdr.detectChanges();
//       },
//     });

//     // 3. Get Real Trade History
//     this.exchangeService.getUserOrders(this.userId).subscribe({
//       next: (orders) => {
//         this.transactions = orders
//           .map((o) => ({
//             id: o.id ? 'TX-' + o.id : 'TX-PENDING',
//             symbol: o.companyId,
//             type: o.type || 'BUY',
//             qty: o.quantity,
//             price: o.price,
//             date: o.createdAt ? new Date(o.createdAt) : new Date(),
//             status: o.status,
//           }))
//           .reverse();

//         this.cdr.detectChanges(); // 🟢 Notify Angular that transactions list is ready
//       },
//       error: (err) => {
//         console.error('Could not fetch trade history', err);
//         this.cdr.detectChanges();
//       },
//     });
//   }

//   calculateStats() {
//     this.totalBalance = this.holdings.reduce((acc, curr) => acc + (curr.marketValue || 0), 0);
//     this.totalPnL = this.holdings.reduce((acc, curr) => acc + (curr.pnl || 0), 0);
//   }
// }

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { PortfolioService, PortfolioResponse } from '../../services/portfolio';
import { CompanyService } from '../../services/company';
import { Company } from '../../models/company';
import { ExchangeService } from '../../services/exchange';
import { PortfolioStateService } from '../../services/profile-state.service.ts';
import { Trading } from '../trading/trading'; // 🟢 Import Trading Component
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.html',
  standalone: true,
  imports: [CommonModule, FormsModule, Trading], // 🟢 Add Trading to imports
})
export class Wallet implements OnInit {
  userId = 2;
  selectedPortfolioId = 1;
  portfolioIds: number[] = [1];

  // 🟢 State for selection
  selectedStock: any = null;

  holdings: any[] = [];
  transactions: any[] = [];
  isLoading = true;
  totalBalance = 0;
  totalPnL = 0;

  constructor(
    public portfolioService: PortfolioService,
    private companyService: CompanyService,
    private exchangeService: ExchangeService,
    private portfolioState: PortfolioStateService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.portfolioState.selectedId$.subscribe((id) => {
      this.selectedPortfolioId = id;
      this.loadData();
    });
  }

  // 🟢 Click handler for table rows
  selectHolding(stock: any) {
    this.selectedStock = {
      symbol: stock.companyId,
      price: stock.currentPrice,
    };
    this.cdr.detectChanges();
  }

  loadData() {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.portfolioService.getPortfolio(this.userId, this.selectedPortfolioId).subscribe({
      next: (holdings: PortfolioResponse[]) => {
        this.companyService.getAllCompanies().subscribe((companies: Company[]) => {
          this.holdings = holdings.map((h) => {
            const marketData = companies.find((c) => c.shortId === h.companyId);
            const currentPrice = marketData ? marketData.currentPrice : h.averageBuyPrice;
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
    });

    this.exchangeService.getUserOrders(this.userId).subscribe((orders) => {
      this.transactions = orders
        .map((o) => ({
          symbol: o.companyId,
          type: o.type,
          qty: o.quantity,
          price: o.price,
          date: o.createdAt ? new Date(o.createdAt) : new Date(),
          status: o.status,
        }))
        .reverse();
      this.cdr.detectChanges();
    });
  }

  changePortfolio(id: number) {
    this.portfolioState.setPortfolio(id);
    this.selectedStock = null; // Clear trading view on switch
  }

  addNewPortfolio() {
    const nextId = Math.max(...this.portfolioIds) + 1;
    this.portfolioIds.push(nextId);
    this.portfolioState.setPortfolio(nextId);
  }

  calculateStats() {
    this.totalBalance = this.holdings.reduce((acc, curr) => acc + (curr.marketValue || 0), 0);
    this.totalPnL = this.holdings.reduce((acc, curr) => acc + (curr.pnl || 0), 0);
  }
}
