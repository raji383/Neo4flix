import { inject } from '@angular/core';
import {
    HttpErrorResponse,
    HttpInterceptorFn
} from '@angular/common/http';

import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/internal/operators/catchError';
import { throwError } from 'rxjs';
import { ErrorMessageService } from '../services/error-message.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
 const token = localStorage.getItem('jwtToken');
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const errorMessageService = inject(ErrorMessageService);

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const backendMessage = errorMessageService.getHttpErrorMessage(error);
console.log(error);

      if (error.status === 401 && token) {
        localStorage.removeItem('jwtToken');
        router.navigate(['/login']);
      }
  
      if (error.status === 403) {
       
        snackBar.open(backendMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'bottom',
        });
        return throwError(() => error);
      }

      snackBar.open(backendMessage, 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar'],
        horizontalPosition: 'end',
        verticalPosition: 'bottom',
      });

      return throwError(() => error);
    }),
  );
};