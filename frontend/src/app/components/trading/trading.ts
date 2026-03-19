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
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';

@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
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
  priceHistory: { price: number; time: Date }[] = [];
  private readonly MAX_HISTORY = 50;

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
  ) { }

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
    this.priceSubscription?.unsubscribe();
    if (!this.companyId) return;

    // First load historical data, then start SSE
    this.stockStream.getHistory(this.companyId).subscribe({
      next: (history) => {
        this.priceHistory = history.map(h => ({
          price: h.price,
          time: new Date(h.recordedAt)
        }));
        this.cdr.detectChanges();

        // Now start live stream appending on top of history
        this.priceSubscription = this.stockStream
          .streamPrice(this.companyId)
          .subscribe(price => {
            this.currentPrice = price;
            this.priceHistory = [
              ...this.priceHistory.slice(-(this.MAX_HISTORY - 1)),
              { price, time: new Date() }
            ];
            this.cdr.detectChanges();
          });
      },
      error: (err) => {
        console.error('Failed to load price history:', err);
        // Fall back to just the live stream if history fails
        this.priceSubscription = this.stockStream
          .streamPrice(this.companyId)
          .subscribe(price => {
            this.currentPrice = price;
            this.priceHistory = [
              ...this.priceHistory.slice(-(this.MAX_HISTORY - 1)),
              { price, time: new Date() }
            ];
            this.cdr.detectChanges();
          });
      }
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

  get chartData(): ChartData<'line'> {
    return {
      labels: this.priceHistory.map(p =>
        p.time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
      ),
      datasets: [{
        data: this.priceHistory.map(p => p.price),
        borderColor: this.isPriceUp ? '#34d399' : '#f43f5e',
        backgroundColor: this.isPriceUp
          ? 'rgba(52, 211, 153, 0.08)'
          : 'rgba(244, 63, 94, 0.08)',
        borderWidth: 2,
        pointRadius: 0,
        pointHoverRadius: 4,
        fill: true,
        tension: 0.4,
      }]
    };
  }

  get isPriceUp(): boolean {
    if (this.priceHistory.length < 2) return true;
    return this.priceHistory[this.priceHistory.length - 1].price >=
      this.priceHistory[this.priceHistory.length - 2].price;
  }

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 300 },
    plugins: {
      legend: { display: false },
      tooltip: {
        mode: 'index',
        intersect: false,
        backgroundColor: '#1e293b',
        titleColor: '#94a3b8',
        bodyColor: '#f1f5f9',
        callbacks: {
          label: ctx => `₹${ctx.parsed.y?.toFixed(2)}`
        }
      }
    },
    scales: {
      x: {
        grid: { color: 'rgba(148,163,184,0.05)' },
        ticks: {
          color: '#475569',
          maxTicksLimit: 6,
          font: { size: 10 }
        }
      },
      y: {
        position: 'right',
        grid: { color: 'rgba(148,163,184,0.05)' },
        ticks: {
          color: '#475569',
          font: { size: 10 },
          callback: val => `₹${Number(val).toFixed(2)}`
        }
      }
    }
  };
}
