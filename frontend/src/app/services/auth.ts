import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { ApiResponse, AuthResponse, LoginDTO, User } from '../models/auth';

export interface RegisterDTO {
  fullName: string;
  username: string;
  email: string;
  phoneNumber: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class  AuthService {
  // Points to API Gateway (proxied from 4200→8080)
  private baseUrl = '/api/v1/auth';
  private readonly TOKEN_KEY = 'token';
  private readonly REFRESH_TOKEN_KEY = 'refreshToken';
  private readonly USER_KEY = 'currentUser';

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  /**
   * Register a new user
   * @param registerData - User registration data
   * @returns Observable of ApiResponse with AuthResponse
   */
  register(registerData: RegisterDTO): Observable<ApiResponse<AuthResponse>> {
    console.log('🔐 Sending register request to:', `${this.baseUrl}/register`);
    console.log('📦 Payload:', registerData);

    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.baseUrl}/register`, registerData)
      .pipe(
        tap((res) => {
          console.log('✅ Register successful:', res);
          if (res.success && res.data) {
            this.saveAuthTokens(res.data);
            this.saveUser(res.data.user);
          }
        }),
        catchError((error) => this.handleError(error, 'Registration'))
      );
  }

  /**
   * Login with email/username and password
   * @param credentials - Login credentials
   * @returns Observable of ApiResponse with AuthResponse
   */
  login(credentials: LoginDTO): Observable<ApiResponse<AuthResponse>> {
    console.log('🔑 Sending login request to:', `${this.baseUrl}/login`);
    console.log('📦 Payload:', { ...credentials, password: '***' });

    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, credentials)
      .pipe(
        tap((res) => {
          console.log('✅ Login successful:', res);
          if (res.success && res.data) {
            this.saveAuthTokens(res.data);
            this.saveUser(res.data.user);
          }
        }),
        catchError((error) => this.handleError(error, 'Login'))
      );
  }

  /**
   * Get the access token
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Get the refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get currently logged-in user
   */
  getCurrentUser(): User | null {
    const userJson = localStorage.getItem(this.USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Logout and clear all auth data
   */
  logout(): void {
    console.log('🚪 Logging out...');
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    console.log('✅ Logout complete. Auth data cleared.');
  }

  /**
   * Save authentication tokens to localStorage
   */
  private saveAuthTokens(authResponse: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResponse.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, authResponse.refreshToken);
    console.log('💾 Tokens saved to localStorage');
  }

  /**
   * Save user data to localStorage
   */
  private saveUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    console.log('💾 User data saved:', user);
  }

  /**
   * Load user from localStorage (called on service init)
   */
  private loadUserFromStorage(): void {
    const userJson = localStorage.getItem(this.USER_KEY);
    if (userJson) {
      console.log('📂 User loaded from storage:', JSON.parse(userJson));
    }
  }

  /**
   * Centralized error handling
   */
  private handleError(error: HttpErrorResponse, context: string) {
    console.error(`❌ ${context} Error:`, error);
    console.error('Status:', error.status);
    console.error('Message:', error.message);
    console.error('Body:', error.error);

    let message = 'An error occurred. Please try again.';

    if (error.status === 0) {
      message = 'Connection error. Please check if the backend is running.';
    } else if (error.status === 400) {
      message = error.error?.message || 'Invalid request. Check your input.';
    } else if (error.status === 401) {
      message = 'Invalid credentials.';
    } else if (error.status === 409) {
      message = 'User already exists.';
    } else if (error.status === 500) {
      message = 'Server error. Please try again later.';
    } else if (error.status === 503) {
      message = 'Service unavailable. Backend might be offline.';
    }

    return throwError(() => ({
      status: error.status,
      message: message,
      details: error.error,
    }));
  }
}


