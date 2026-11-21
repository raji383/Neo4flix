import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ErrorMessageService } from './error-message.service';

type NotificationKind = 'error' | 'success' | 'info';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);
  private readonly errorMessageService = inject(ErrorMessageService);

  showError(message: string): void {
    this.open(message, 'error');
  }

  showHttpError(error: unknown, fallback?: string): void {
    this.showError(this.errorMessageService.getHttpErrorMessage(error, fallback));
  }

  showSuccess(message: string): void {
    this.open(message, 'success');
  }

  showInfo(message: string): void {
    this.open(message, 'info');
  }

  private open(message: string, kind: NotificationKind): void {
    this.snackBar.open(message, 'Close', {
      duration: kind === 'success' ? 3500 : 5000,
      horizontalPosition: 'end',
      verticalPosition: 'bottom',
      panelClass: ['app-snackbar', `app-snackbar--${kind}`],
    });
  }
}
