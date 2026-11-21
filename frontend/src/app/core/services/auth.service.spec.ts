import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../../shared/models/auth';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const response: AuthResponse = {
    id: 'user-123',
    username: 'test-user',
    email: 'test@example.com',
    role: 'USER',
    jwtToken: 'sample-token',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    localStorage.removeItem('jwtToken');
  });

  afterEach(() => {
    http.verify();
    localStorage.removeItem('jwtToken');
  });

  it('posts login credentials and stores the returned token', () => {
    const credentials = { username: 'test-user', password: 'secret' };

    service.login(credentials).subscribe((result) => expect(result).toEqual(response));

    const request = http.expectOne(`${environment.apiUrl}/api/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(credentials);
    request.flush(response);

    expect(service.getToken()).toBe('sample-token');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('registers a user without storing an authentication token', () => {
    const registration = {
      username: 'test-user',
      email: 'test@example.com',
      password: 'secret',
      confirmPassword: 'secret',
    };

    service.register(registration).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/api/auth/register`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(registration);
    request.flush({
      id: 'user-123',
      username: 'test-user',
      email: 'test@example.com',
      role: 'USER',
      message: 'Registration successful. You can now sign in.',
    });

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('clears the stored token on logout', () => {
    localStorage.setItem('jwtToken', 'sample-token');

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
