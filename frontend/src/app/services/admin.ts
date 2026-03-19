import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Company {
  id?: string;
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

  deleteCompany(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/companies/${id}`, {
      headers: this.headers(),
    });
  }

  updatePrice(id: string, price: number): Observable<Company> {
    // Setting price as a query parameter (?price=...) as seen in Swagger
    const params = new HttpParams().set('price', price.toString());

    return this.http.put<Company>(`${this.base}/companies/${id}/price`, null, {
      headers: this.headers(),
      params: params,
    });
  }
}
