import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { afterEach, vi } from 'vitest';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  const snackBar = { open: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: snackBar }],
    });
  });

  afterEach(() => TestBed.resetTestingModule());

  it('shows HTTP errors with the shared error style', () => {
    const notifications = TestBed.inject(NotificationService);

    notifications.showHttpError(new HttpErrorResponse({ status: 403 }), 'Access denied.');

    expect(snackBar.open).toHaveBeenCalledWith(
      'Access denied.',
      'Close',
      expect.objectContaining({
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'bottom',
        panelClass: ['app-snackbar', 'app-snackbar--error'],
      }),
    );
  });

  it('shows success messages with the shared success style', () => {
    const notifications = TestBed.inject(NotificationService);

    notifications.showSuccess('Saved.');

    expect(snackBar.open).toHaveBeenCalledWith(
      'Saved.',
      'Close',
      expect.objectContaining({
        duration: 3500,
        panelClass: ['app-snackbar', 'app-snackbar--success'],
      }),
    );
  });
});
