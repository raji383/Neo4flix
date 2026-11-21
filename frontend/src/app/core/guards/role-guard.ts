

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { NotificationService } from '../services/notification.service';

interface TokenPayload {
  sub: string;
  role: string[];
  exp: number;
}
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const router = inject(Router);
    const notifications = inject(NotificationService);
    const token = localStorage.getItem('jwtToken');

    if (!token) {
      notifications.showError('You must be logged in to view this page.');
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
      notifications.showError('You do not have permission to view this page.');
      router.navigate(['/dashboard']);
      return false;

    } catch (error) {
      notifications.showError('Your session is invalid. Please sign in again.');
      router.navigate(['/login']);
      return false;
    }
  };
};
