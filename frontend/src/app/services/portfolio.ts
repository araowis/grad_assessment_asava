import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
export interface PortfolioRequest {
  portfolioGroupId: number;
  companyId: string;
  price: number;
  quantity: number;
  stopLoss?: number;
}

export interface PortfolioResponse {
  companyId: string;
  quantity: number;
  averageBuyPrice: number;
}
// import { PortfolioRequest, PortfolioResponse } from '../models/portfolio';

@Injectable({
  providedIn: 'root',
})
export class PortfolioService {
  // Uses Gateway Port 8088: http://localhost:8088/api/v1/portfolios
  private baseUrl = `${environment.baseUrl}/portfolios`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  /**
   * BUY: POST /api/v1/portfolios/{userId}/buy
   */
  buyStock(userId: number, request: PortfolioRequest): Observable<PortfolioResponse> {
    return this.http.post<PortfolioResponse>(`${this.baseUrl}/${userId}/buy`, request, {
      headers: this.getHeaders(),
    });
  }

  /**
   * SELL: POST /api/v1/portfolios/{userId}/sell
   */
  sellStock(userId: number, request: PortfolioRequest): Observable<PortfolioResponse> {
    return this.http.post<PortfolioResponse>(`${this.baseUrl}/${userId}/sell`, request, {
      headers: this.getHeaders(),
    });
  }

  /**
   * GET USER HOLDINGS: GET /api/v1/portfolios/{userId}/{portfolioId}
   */
  getPortfolio(userId: number, portfolioId: number): Observable<PortfolioResponse[]> {
    return this.http.get<PortfolioResponse[]>(`${this.baseUrl}/${userId}/${portfolioId}`, {
      headers: this.getHeaders(),
    });
  }
}
