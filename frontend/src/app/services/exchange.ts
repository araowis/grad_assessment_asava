import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderRequest,OrderResponse,TradeResponse } from '../models/exchange';
@Injectable({
  providedIn: 'root',
})
export class ExchangeService {
  private baseUrl = 'http://localhost:8081/api/v1/exchange'; // Adjust if different

  constructor(private http: HttpClient) { }

  placeOrder(order: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders`, order);
  }

  // Price validation logic (5% deviation)
  validatePrice(currentPrice: number, orderPrice: number): { valid: boolean; message: string } {
    const deviation = Math.abs(orderPrice - currentPrice) / currentPrice;
    if (deviation > 0.05) {
      return { valid: false, message: 'Price deviation cannot exceed 5% of current market price.' };
    }
    return { valid: true, message: '' };
  }
}
