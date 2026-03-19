import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExchangeService } from '../../services/exchange';
import { OrderRequest } from '../../models/exchange';
@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trading.html',
  styleUrl: './trading.css',
})
export class Trading {
  selectedTimeframe: string = '4H';
  // currentPrice = 150.00; // Mock current price from backend

  @Input() companyId!: string;
  @Input() currentPrice!: number;
  @Input() userId!: number;

  order: OrderRequest = {
    userId: 1, // Get from Auth session
    companyId: 'AAPL',
    quantity: 1,
    price: 150.00,
    type: 'BUY'
  };

  constructor(private exchangeService: ExchangeService) {}

  ngOnInit() {
    this.order.companyId = this.companyId;
    this.order.price = this.currentPrice;
  }

  setType(type: 'BUY' | 'SELL') {
    this.order.type = type;
  }

  setTimeframe(time: string) {
    this.selectedTimeframe = time;
    console.log(`Timeframe changed to: ${time}`);
    // You can call a service here later to fetch new chart data
  }

  submitOrder() {
    const validation = this.exchangeService.validatePrice(this.currentPrice, this.order.price);
    
    if (!validation.valid) {
      alert(validation.message);
      return;
    }

    this.exchangeService.placeOrder(this.order).subscribe({
      next: () => {
        alert(`${this.order.type} order placed successfully!`);
      },
      error: (err) => {
        alert('Order Failed: ' + (err.error?.message || 'Unknown error'));
      }
    });
  }
}
