import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { ApiResponse, AuthResponse, LoginDTO, RegisterDTO, User } from '../models/auth';
import { handleError } from '../utils/error.handler';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private baseUrl = 'http://localhost:8088/api/v1/auth';

  constructor(private http: HttpClient) {}

  register(registerData: RegisterDTO): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/register`, registerData).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.saveAuthTokens(res.data);
          this.saveUser(res.data.user);
        }
      }),
      catchError((error) => handleError(error, 'Registration')),
    );
  }

  login(credentials: LoginDTO): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, credentials).pipe(
      tap((res: ApiResponse<AuthResponse>) => {
        if (res.success && res.data) {
          this.saveAuthTokens(res.data);
          this.saveUser(res.data.user);
        }
      }),
    );
  }

  getToken(): string | null {
    // Matches the key set in saveAuthTokens
    return localStorage.getItem('token');
  }

  logout(): void {
    localStorage.clear();
    // Logic for calling backend logout can be added here if needed
  }

  getCurrentUser(): User | null {
    // Synchronized with the key used in Guards
    const userData = localStorage.getItem('currentUser');
    if (!userData) return null;
    try {
      return JSON.parse(userData);
    } catch {
      return null;
    }
  }

  private saveAuthTokens(data: AuthResponse) {
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
  }

  private saveUser(user: User) {
    // Using 'currentUser' to match your Guards
    localStorage.setItem('currentUser', JSON.stringify(user));
  }
}
