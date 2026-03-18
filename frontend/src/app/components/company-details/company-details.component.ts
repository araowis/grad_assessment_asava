import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions, ChartType } from 'chart.js';
import 'chart.js/auto';
import { interval, Subscription } from 'rxjs';

interface Transaction {
  type: 'buy' | 'sell';
  amount: number;
  price: number;
  time: string;
}

interface OrderData {
  price: number;
  amount: number;
}

interface OrderBook {
  asks: OrderData[];
  bids: OrderData[];
}

@Component({
  selector: 'app-company-details',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, FormsModule],
  templateUrl: './company-details.component.html',
  styleUrls: ['./company-details.component.css'],
})
export class CompanyDetailsComponent implements OnInit, OnDestroy {
  @Input() symbol = 'BTC';
  @Input() name = 'Bitcoin';

  constructor(private route: ActivatedRoute) {}

  // Price Data
  price = 64321.0;
  changePercent = 0.0;

  // Time Period
  timePeriods = ['1H', '1D', '1W', '1M', '3M', '1Y'];
  selectedPeriod = '1D';

  // Price Stats
  priceStats = {
    high24h: 65000,
    low24h: 63500,
    volume24h: 2500000000,
    marketCap: 1200000000000,
  };

  // Chart
  chartType: 'line' = 'line';
  chartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Price',
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        pointRadius: 0,
        borderWidth: 2,
        tension: 0.3,
        fill: true,
      },
    ],
  };

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 300,
    },
    scales: {
      x: {
        display: true,
        ticks: {
          maxTicksLimit: 6,
          color: '#64748b',
          font: { size: 10 },
        },
        grid: {
          color: 'rgba(30, 41, 59, 0.5)',
        },
      },
      y: {
        display: true,
        ticks: {
          callback: (value) => `$${value}`,
          color: '#64748b',
          font: { size: 10 },
        },
        grid: {
          color: 'rgba(30, 41, 59, 0.5)',
        },
      },
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: '#334155',
        borderWidth: 1,
        padding: 12,
        titleColor: '#e2e8f0',
        bodyColor: '#cbd5e1',
        callbacks: {
          label: (context: any) => {
            const value = context.parsed?.y ?? 0;
            return `$${value.toFixed(2)}`;
          },
        },
      },
    },
  };

  // Order Book
  orderBook: OrderBook = {
    asks: [
      { price: 64325, amount: 1.25 },
      { price: 64330, amount: 0.85 },
      { price: 64335, amount: 1.5 },
      { price: 64340, amount: 2.0 },
      { price: 64345, amount: 0.7 },
    ],
    bids: [
      { price: 64320, amount: 1.1 },
      { price: 64315, amount: 0.95 },
      { price: 64310, amount: 1.8 },
      { price: 64305, amount: 1.6 },
      { price: 64300, amount: 0.9 },
    ],
  };

  // Recent Transactions
  recentTransactions: Transaction[] = [
    { type: 'buy', amount: 0.5, price: 64200, time: '2 min ago' },
    { type: 'sell', amount: 1.2, price: 64150, time: '5 min ago' },
    { type: 'buy', amount: 0.8, price: 64100, time: '10 min ago' },
    { type: 'sell', amount: 0.3, price: 64050, time: '15 min ago' },
    { type: 'buy', amount: 1.5, price: 64000, time: '20 min ago' },
  ];

  // Order Form
  orderType: 'buy' | 'sell' = 'buy';
  orderQuantity = 0;
  orderPrice = 64321;

  // Computed
  get currentSpread(): number {
    if (this.orderBook.asks.length === 0 || this.orderBook.bids.length === 0) return 0;
    const highestBid = this.orderBook.bids[0].price;
    const lowestAsk = this.orderBook.asks[0].price;
    return ((lowestAsk - highestBid) / highestBid) * 100;
  }

  private updateSub?: Subscription;

  ngOnInit(): void {
    // Read from route
    const symbolFromRoute = this.route.snapshot.paramMap.get('symbol');
    if (symbolFromRoute) {
      this.symbol = symbolFromRoute.toUpperCase();
    }

    const nameFromQuery = this.route.snapshot.queryParamMap.get('name');
    if (nameFromQuery) {
      this.name = nameFromQuery;
    } else {
      const mappedNames: Record<string, string> = {
        BTC: 'Bitcoin',
        ETH: 'Ethereum',
        SOL: 'Solana',
      };
      this.name = mappedNames[this.symbol.toUpperCase()] ?? this.name;
    }

    // Set initial prices and stats based on symbol
    this.setInitialPrice();

    // Initialize chart
    this.initializeMockData();
    this.startLiveUpdates();
  }

  private setInitialPrice(): void {
    const basePrices: Record<string, number> = {
      BTC: 64321,
      ETH: 3452,
      SOL: 145,
    };
    this.price = basePrices[this.symbol.toUpperCase()] ?? this.price;
    this.orderPrice = this.price;
    this.changePercent = 0;

    // Update price stats based on symbol
    const priceStatsMap: Record<string, typeof this.priceStats> = {
      BTC: {
        high24h: 65000,
        low24h: 63500,
        volume24h: 2500000000,
        marketCap: 1200000000000,
      },
      ETH: {
        high24h: 3500,
        low24h: 3400,
        volume24h: 15000000000,
        marketCap: 415000000000,
      },
      SOL: {
        high24h: 150,
        low24h: 140,
        volume24h: 1200000000,
        marketCap: 67000000000,
      },
    };
    this.priceStats = priceStatsMap[this.symbol.toUpperCase()] ?? this.priceStats;
  }

  selectTimePeriod(period: string): void {
    this.selectedPeriod = period;
    // In a real app, this would trigger a new API call with different time period data
  }

  submitOrder(): void {
    if (this.orderQuantity <= 0 || this.orderPrice <= 0) {
      alert('Please enter valid quantity and price');
      return;
    }

    const total = this.orderQuantity * this.orderPrice * 1.001;
    const action = this.orderType === 'buy' ? 'BUY' : 'SELL';

    alert(
      `${action} Order Placed!\nSymbol: ${this.symbol}\nQuantity: ${this.orderQuantity}\nPrice: $${this.orderPrice}\nTotal: $${total.toFixed(2)}`
    );

    // Reset form
    this.orderQuantity = 0;
  }

  ngOnDestroy(): void {
    this.updateSub?.unsubscribe();
  }

  private initializeMockData(): void {
    const now = new Date();
    const initialPoints = 12;

    for (let i = initialPoints; i > 0; i--) {
      const timeLabel = new Date(now.getTime() - i * 2000).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
      this.chartData.labels?.push(timeLabel);
      this.chartData.datasets[0].data.push(this.price);
    }
  }

  private startLiveUpdates(): void {
    this.updateSub = interval(2000).subscribe(() => {
      const delta = (Math.random() - 0.5) * 200;
      this.price = Math.max(0, this.price + delta);
      const lastPrice = (this.chartData.datasets[0].data.slice(-1)[0] as number) ?? this.price;
      this.changePercent = ((this.price - lastPrice) / lastPrice) * 100;

      const now = new Date();
      const timeLabel = now.toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });

      this.chartData.labels?.push(timeLabel);
      (this.chartData.datasets[0].data as number[]).push(this.price);

      // Keep only last 20 data points
      if (this.chartData.labels && this.chartData.labels.length > 20) {
        this.chartData.labels.shift();
        (this.chartData.datasets[0].data as number[]).shift();
      }
    });
  }
}

