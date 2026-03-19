import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

/**
 * AuthGuard — protects trader/analyst routes.
 * - ROLE_TRADER  → allowed
 * - ROLE_ANALYST → allowed
 * - ROLE_ADMIN   → redirected to /admin/dashboard (they have their own area)
 * - Not logged in → redirected to /login
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user   = JSON.parse(localStorage.getItem('currentUser') || '{}');

  if (user?.role === 'ROLE_TRADER' || user?.role === 'ROLE_ANALYST') {
    return true;
  }

  if (user?.role === 'ROLE_ADMIN') {
    router.navigate(['/admin/dashboard']);
    return false;
  }

  router.navigate(['/login']);
  return false;
};