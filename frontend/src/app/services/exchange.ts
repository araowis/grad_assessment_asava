import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderRequest,OrderResponse,TradeResponse } from '../models/exchange';
@Injectable({
  providedIn: 'root',
})
export class ExchangeService {
  /**
   * Use a relative path + proxy configuration for local development.
   *
   * The API Gateway should be listening on http://localhost:8080.
   */
  private baseUrl = '/api/v1/exchange';

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
