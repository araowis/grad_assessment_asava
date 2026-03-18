import { Component } from '@angular/core';
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
  currentPrice = 150.00; // Mock current price from backend
  order: OrderRequest = {
    userId: 1, // Get from Auth session
    companyId: 'AAPL',
    quantity: 1,
    price: 150.00,
    type: 'BUY'
  };

  constructor(private exchangeService: ExchangeService) {}

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
      next: (res) => alert('Order Placed Successfully!'),
      error: (err) => alert('Order Failed: ' + err.error.message)
    });
  }
}
