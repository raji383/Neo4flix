import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { afterEach, vi } from 'vitest';
import { guestGuard } from './guest-guard';

describe('guestGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => guestGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    });
  });

  afterEach(() => {
    localStorage.removeItem('jwtToken');
    TestBed.resetTestingModule();
  });

  it('redirects authenticated users to the existing movies route', () => {
    localStorage.setItem('jwtToken', 'token');

    const result = executeGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/login' } as RouterStateSnapshot,
    );

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      '/movies',
    );
  });

  it('allows unauthenticated users to open the login route', () => {
    const result = executeGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/login' } as RouterStateSnapshot,
    );

    expect(result).toBe(true);
  });
});
