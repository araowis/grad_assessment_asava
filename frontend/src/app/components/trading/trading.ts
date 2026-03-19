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
import { Subject, takeUntil } from 'rxjs';
import { ExchangeService } from '../../services/exchange';
import { PortfolioService, PortfolioRequest } from '../../services/portfolio';

@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trading.html',
  styleUrl: './trading.css',
})
export class Trading implements OnInit, OnChanges, OnDestroy {
  private destroy$ = new Subject<void>();
  assetTransactions: any[] = [];
  isLoadingOrder = false;
  selectedTimeframe: string = '1D';

  @Input() companyId!: string;
  @Input() currentPrice!: number;
  @Input() userId!: number;
  @Input() portfolioGroupId: number = 1;
  @Input() mode: 'exchange' | 'portfolio' = 'exchange';

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
  ) {}

  ngOnInit() {
    this.updateOrderFromInputs();
    this.loadAssetActivity();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['companyId'] || changes['userId']) {
      this.updateOrderFromInputs();
      this.loadAssetActivity();
    }

    if (changes['currentPrice'] && this.currentPrice) {
      this.syncPrice();
    }
    // Force UI to update when @Inputs change
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAssetActivity() {
    if (!this.userId || !this.companyId) return;

    this.exchangeService
      .getUserOrders(this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (orders) => {
          this.assetTransactions = orders
            .filter((o: any) => o.companyId === this.companyId)
            .map((o: any) => ({
              ...o,
              date: o.createdAt || new Date(),
            }))
            .reverse();
          this.cdr.detectChanges(); // Update list after data arrives
        },
      });
  }

  private syncPrice() {
    const roundedMarketPrice = this.roundPrice(this.currentPrice);
    if (!this.order.price || this.order.price === 0) {
      this.order.price = roundedMarketPrice;
      this.cdr.detectChanges();
    }
  }

  private updateOrderFromInputs() {
    this.order.companyId = this.companyId || '';
    this.order.userId = this.userId || 0;
    if (this.mode === 'exchange') this.order.type = 'BUY';
    this.cdr.detectChanges();
  }

  submitOrder() {
    if (!this.companyId || !this.userId) {
      alert('Missing User ID or Company Symbol.');
      return;
    }

    this.isLoadingOrder = true;
    this.cdr.detectChanges(); // Show spinner immediately

    this.order.price = this.roundPrice(this.order.price);
    const finalStopLoss = this.order.stopLoss ? this.roundPrice(this.order.stopLoss) : 0;

    if (this.mode === 'portfolio') {
      const portfolioReq: PortfolioRequest = {
        portfolioGroupId: this.portfolioGroupId,
        companyId: this.companyId,
        quantity: this.order.quantity,
        price: this.order.price,
        stopLoss: finalStopLoss,
        orderType: this.order.type,
      };

      const request$ =
        this.order.type === 'BUY'
          ? this.portfolioService.buyStock(this.userId, portfolioReq)
          : this.portfolioService.sellStock(this.userId, portfolioReq);

      this.handleSubscription(request$, 'Portfolio');
    } else {
      const exchangeReq = {
        userId: this.userId,
        companyId: this.companyId,
        quantity: this.order.quantity,
        price: this.order.price,
        orderType: this.order.type,
      };

      this.handleSubscription(this.exchangeService.placeOrder(exchangeReq), 'Exchange');
    }
  }

  private handleSubscription(obs$: any, source: string) {
    obs$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isLoadingOrder = false;
        this.loadAssetActivity();
        alert(`Success! ${this.order.type} order processed.`);
        this.cdr.detectChanges(); // Hide spinner and refresh UI
      },
      error: (err: any) => {
        this.isLoadingOrder = false;
        const errorMsg = err.error?.message || err.message || 'Server Error';
        alert(`${source} Error: ` + errorMsg);
        this.cdr.detectChanges(); // Ensure error state is shown/handled
      },
    });
  }

  private roundPrice(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }
}
