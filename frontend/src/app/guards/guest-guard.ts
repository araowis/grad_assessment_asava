import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * GuestGuard: prevents authenticated users from seeing the landing page,
 * login, or register screens. Redirects them to their role-appropriate dashboard.
 *
 * Usage: canActivate: [guestGuard] on the landing, /login, /register routes.
 */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  const user  = JSON.parse(localStorage.getItem('currentUser') || '{}');

  if (!token || !user?.role) {
    return true;
  }

  if (user.role === 'ROLE_ADMIN') {
    router.navigate(['/admin/dashboard']);
  } else {
    router.navigate(['/app/dashboard']);
  }

  return false;
};