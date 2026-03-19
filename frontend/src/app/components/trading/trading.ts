import {
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExchangeService } from '../../services/exchange';
import { PortfolioService, PortfolioRequest } from '../../services/portfolio';
import { StockStreamService } from '../../services/stock-stream';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trading.html',
  styleUrl: './trading.css',
})
export class Trading implements OnInit, OnChanges, OnDestroy {
  // This is the local list for the terminal table
  assetTransactions: any[] = [];

  @Input() companyId!: string;
  @Input() currentPrice!: number;
  @Input() userId!: number;
  @Input() portfolioGroupId: number = 1;
  @Input() mode: 'exchange' | 'portfolio' = 'exchange';

  selectedTimeframe: string = '1D';
  isLoadingOrder = false;
  private priceSubscription?: Subscription;

  order = {
    userId: 0,
    companyId: '',
    quantity: 1,
    price: 0,
    stopLoss: 0,
    type: 'BUY' as 'BUY' | 'SELL',
  };

  constructor(
    private exchangeService: ExchangeService,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef,
    private stockStream: StockStreamService
  ) {}

  ngOnInit() {
    this.updateOrderFromInputs();
    this.loadAssetActivity();
    this.subscribeToPrice();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['companyId'] || changes['currentPrice'] || changes['userId']) {
      this.updateOrderFromInputs();
      this.loadAssetActivity();
    }
    if (changes['companyId'] && !changes['companyId'].isFirstChange()) {
        this.subscribeToPrice(); 
      }
  }

  ngOnDestroy() {
    this.priceSubscription?.unsubscribe();
  }

  private subscribeToPrice() {
    this.priceSubscription?.unsubscribe(); // drop old stream
    console.log('subscribeToPrice called, companyId:', this.companyId);

    if (!this.companyId) return;

    this.priceSubscription = this.stockStream
      .streamPrice(this.companyId)
      .subscribe(price => {
        this.currentPrice = price;
        this.cdr.detectChanges();
      });
  }

  setTimeframe(time: string) {
    this.selectedTimeframe = time;
    this.cdr.detectChanges();
  }

  private roundPrice(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  // Fetches real trade history specifically for this asset
  private loadAssetActivity() {
    if (!this.userId || !this.companyId) return;

    this.exchangeService.getUserOrders(this.userId).subscribe({
      next: (orders) => {
        this.assetTransactions = orders
          .filter((o) => o.companyId === this.companyId)
          .map((o) => ({
            ...o,
            date: o.createdAt || new Date(),
          }))
          .reverse();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Terminal activity fetch failed', err),
    });
  }

  private updateOrderFromInputs() {
  if (this.companyId) this.order.companyId = this.companyId;
  if (this.userId) this.order.userId = this.userId;

  if (this.order.price === 0 && this.currentPrice) {
    this.order.price = this.roundPrice(this.currentPrice);
  }
  this.cdr.detectChanges();
}

  submitOrder() {
    this.order.companyId = this.companyId;
    this.order.userId = this.userId;
    this.order.price = this.roundPrice(this.order.price);
    const finalStopLoss = this.order.stopLoss ? this.roundPrice(this.order.stopLoss) : 0;

    if (!this.order.companyId || !this.order.userId) {
      alert('Missing User ID or Company Symbol.');
      return;
    }

    this.isLoadingOrder = true;
    this.cdr.detectChanges();

    if (this.mode === 'portfolio') {
      const portfolioReq: PortfolioRequest = {
        portfolioGroupId: this.portfolioGroupId,
        companyId: this.order.companyId,
        quantity: this.order.quantity,
        price: this.order.price,
        stopLoss: finalStopLoss,
      };

      const request$ =
        this.order.type === 'BUY'
          ? this.portfolioService.buyStock(this.userId, portfolioReq)
          : this.portfolioService.sellStock(this.userId, portfolioReq);

      this.handleSubscription(request$, 'Portfolio');
    } else {
      this.handleSubscription(this.exchangeService.placeOrder({ ...this.order }), 'Exchange');
    }
  }

  private handleSubscription(obs$: any, source: string) {
    obs$.subscribe({
      next: () => {
        alert(`Success! Order executed via ${source}.`);
        this.isLoadingOrder = false;
        this.loadAssetActivity(); // Refresh local history after trade
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.isLoadingOrder = false;
        alert(`${source} Error: ` + (err.error?.message || 'Transaction failed.'));
        this.cdr.detectChanges();
      },
    });
  }
}
