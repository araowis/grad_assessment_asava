import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = JSON.parse(localStorage.getItem('currentUser') || '{}');

  if (user?.role === 'ROLE_ADMIN') {
    return true;
  }

  router.navigate(['/login']);
  return false;
};