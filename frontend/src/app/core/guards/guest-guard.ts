import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { NotificationService } from '../services/notification.service';

export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('jwtToken');
  if (token) {
    inject(NotificationService).showInfo('You are already logged in.');
    return router.createUrlTree(['/movies']);
  }
  return true;
};
