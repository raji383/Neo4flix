import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { afterEach, vi } from 'vitest';
import { NotificationService } from '../services/notification.service';
import { roleGuard } from './role-guard';

describe('roleGuard', () => {
  const router = { navigate: vi.fn() };
  const notifications = { showError: vi.fn() };

  const executeGuard = (allowedRoles: string[]) =>
    TestBed.runInInjectionContext(() =>
      roleGuard(allowedRoles)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.removeItem('jwtToken');
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: NotificationService, useValue: notifications },
      ],
    });
  });

  afterEach(() => {
    localStorage.removeItem('jwtToken');
    TestBed.resetTestingModule();
  });

  it('allows a user whose token contains an allowed role', () => {
    const payload = btoa(JSON.stringify({ sub: 'user-123', role: ['ADMIN'] }));
    localStorage.setItem('jwtToken', `header.${payload}.signature`);

    expect(executeGuard(['ADMIN'])).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects a user without a token to login', () => {
    expect(executeGuard(['ADMIN'])).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(notifications.showError).toHaveBeenCalledWith(
      'You must be logged in to view this page.',
    );
  });
});
