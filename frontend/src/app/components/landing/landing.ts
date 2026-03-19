import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.html',
  styles: [`
    @keyframes marquee {
      from { transform: translateX(0); }
      to   { transform: translateX(-50%); }
    }
  `]
})
export class Landing {
  tickers = [
    { symbol: 'INFY',  price: 1482.50, change:  1.24 },
    { symbol: 'TCS',   price: 3748.00, change:  0.87 },
    { symbol: 'RIL',   price: 2820.30, change: -0.43 },
    { symbol: 'HDFC',  price: 1647.80, change:  2.11 },
    { symbol: 'WIPR',  price:  452.10, change: -1.05 },
    { symbol: 'AXSB',  price:  987.60, change:  0.33 },
    { symbol: 'SBIN',  price:  621.45, change:  1.78 },
    { symbol: 'NFTY',  price: 22450.00, change: 0.56 },
    { symbol: 'BAJF',  price: 6832.00, change: -0.22 },
    { symbol: 'ICBK',  price: 1123.75, change:  0.91 },
  ];
}