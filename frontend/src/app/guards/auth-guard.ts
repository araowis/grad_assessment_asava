import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  if (user.role === 'ROLE_TRADER') {
    return true;
  }
  
  router.navigate(['/login']);
  return false;
};
