import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CanActivateFn, Router } from '@angular/router';

export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('jwtToken');
  const snackbar = inject(MatSnackBar);
  if (token) {
    snackbar.open('You are already logged in!', 'Close', { duration: 3000 });
    return router.createUrlTree(['/movies']);
  }
  return true;
};
