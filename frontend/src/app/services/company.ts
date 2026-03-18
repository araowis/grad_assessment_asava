import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Company } from '../models/company';
@Injectable({
  providedIn: 'root',
})

export class CompanyService {
  private baseUrl = 'http://localhost:8088/api/v1/companies';

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
