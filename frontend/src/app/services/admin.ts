import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Company {
  id?: number;
  name: string;
  stockSymbol: string;
  totalStocks: number;
  currentPrice: number;
}

export interface AdminStats {
  totalCompanies: number;
  totalStocksListed: number;
  totalMarketCap: number;
  totalTrades: number;
  recentCompanies: Company[];
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = 'http://localhost:8088/api/v1';

  constructor(private http: HttpClient) {}

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.base}/stats`, {
      headers: this.headers(),
    });
  }

  getCompanies(): Observable<Company[]> {
    return this.http.get<Company[]>(`${this.base}/companies`, {
      headers: this.headers(),
    });
  }

  addCompany(company: Company): Observable<Company> {
    return this.http.post<Company>(`${this.base}/companies`, company, {
      headers: this.headers(),
    });
  }

  deleteCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/companies/${id}`, {
      headers: this.headers(),
    });
  }

  updatePrice(id: number, price: number): Observable<Company> {
    return this.http.patch<Company>(
      `${this.base}/companies/${id}/price`,
      { price },
      { headers: this.headers() }
    );
  }
}