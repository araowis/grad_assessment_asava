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

  constructor(private http: HttpClient) { }

  register(registerData: RegisterDTO): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/register`, registerData)
      .pipe(
        tap((res) => {
          console.log("User registered successfully with role ROLE_TRADER: ", res);
          if (res.success && res.data) {
            this.saveAuthTokens(res.data);
            this.saveUser(res.data.user);
          }
        }),
        catchError((error) => handleError(error, 'Registration'))
      );
  }

  login(credentials: LoginDTO): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, credentials).pipe(
      tap((res: ApiResponse<AuthResponse>) => {
        if (res.success) {
          this.saveAuthTokens(res.data);
          this.saveUser(res.data.user);
        }
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  logout(): Observable<ApiResponse<void>> {
    localStorage.clear();
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/logout`, {});
  }

  getCurrentUser(): User | null {
    const userData = localStorage.getItem('user');
    if (!userData) {
      return null;
    }
    const parsedUser: User = JSON.parse(userData);
    return parsedUser;
  }

  private saveAuthTokens(data: AuthResponse) {
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
  }

  private saveUser(user: User) {
    localStorage.setItem('user', JSON.stringify(user));
  }
}


