import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Company } from '../models/company';
@Injectable({
  providedIn: 'root',
})

export class CompanyService {
  /**
   * Use a relative URL + Angular dev-server proxy for local development.
   *
   * To run the app locally and forward calls to the backend, start the frontend with:
   *   npm start
   *
   * and ensure a proxy configuration (proxy.conf.json) points `/api/*` to the API Gateway.
   */
  private baseUrl = '/api/v1/companies';

  constructor(private http: HttpClient) { }

  getAllCompanies(): Observable<Company[]> {
    return this.http.get<Company[]>(this.baseUrl);
  }

  addCompany(company: Company): Observable<Company> {
    return this.http.post<Company>(this.baseUrl, company);
  }

  updateCompany(shortId: string, company: Company): Observable<Company> {
    return this.http.put<Company>(`${this.baseUrl}/${shortId}`, company);
  }

  deleteCompany(shortId: string): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${shortId}`, { responseType: 'text' });
  }
}
