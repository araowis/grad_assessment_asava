import { HttpInterceptorFn ,HttpRequest, HttpHandlerFn, HttpErrorResponse} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { Auth } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(Auth);
  const token = localStorage.getItem('token');

  // 1. Clone the request and add the header if the token exists
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  // 2. Handle the response and catch 401 errors for token refresh
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('auth/login')) {
        // Here you would normally call your refresh-token logic
        // For now, we'll just redirect to login if the session is dead
        console.error('Unauthorized! Redirecting to login...');
      }
      return throwError(() => error);
    })
  );
};