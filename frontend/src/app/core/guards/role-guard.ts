

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

interface TokenPayload {
  sub: string;
  role: string[];
  exp: number;
}
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const router = inject(Router);
    const token = localStorage.getItem('jwtToken');

    if (!token) {
      router.navigate(['/login']);
      return false;
    }

    try {
      const decodedToken = jwtDecode<TokenPayload>(token);

      // const isExpired = decodedToken.exp * 1000 < Date.now();
      // if (isExpired) {
      //   localStorage.removeItem('jwtToken');
      //   router.navigate(['/login']);
      //   return false;
      // }

      const hasRole = allowedRoles.some(role => decodedToken.role?.includes(role));


      if (hasRole) {
        return true;
      }
      router.navigate(['/dashboard']);
      return false;

    } catch (error) {
      console.error('Invalid token', error);
      router.navigate(['/login']);
      return false;
    }
  };
};
