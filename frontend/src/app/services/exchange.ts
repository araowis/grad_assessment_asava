import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderRequest, OrderResponse } from '../models/exchange';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root',
})
export class ExchangeService {
  // baseUrl is http://localhost:8088/api/v1
  private resourceUrl = `${environment.baseUrl}/exchange`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  /**
   * PLACE ORDER: POST /api/v1/exchange/order
   */
  placeOrder(order: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.resourceUrl}/order`, order, {
      headers: this.getHeaders(),
    });
  }

  /**
   * GET USER ORDERS: GET /api/v1/exchange/user/{userId}
   * Used for the Trade History section in the Wallet
   */
  getUserOrders(userId: number): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.resourceUrl}/user/${userId}`, {
      headers: this.getHeaders(),
    });
  }

  /**
   * Price validation logic (5% deviation)
   * This stays on the frontend for immediate user feedback
   */
  validatePrice(currentPrice: number, orderPrice: number): { valid: boolean; message: string } {
    const deviation = Math.abs(orderPrice - currentPrice) / currentPrice;
    if (deviation > 0.05) {
      return {
        valid: false,
        message: `Price deviation (₹${orderPrice}) cannot exceed 5% of market price (₹${currentPrice}).`,
      };
    }
    return { valid: true, message: '' };
  }
}
