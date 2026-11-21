import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { NotificationService } from '../services/notification.service';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const notifications = inject(NotificationService);

  const token = localStorage.getItem('jwtToken');
  if (token) {
    return true;
  }

  notifications.showError('You must be logged in to view this page.');
  router.navigate(['/login']);
  return false;
};
