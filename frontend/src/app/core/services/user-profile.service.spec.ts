import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UserProfile } from '../../shared/models/user-profile.model';
import { UserProfileService } from './user-profile.service';

describe('UserProfileService', () => {
  let service: UserProfileService;
  let http: HttpTestingController;
  const url = `${environment.apiUrl}/api/users/me`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserProfileService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the current user profile', () => {
    const profile: UserProfile = {
      id: 'user-123',
      username: 'test-user',
      email: 'test@example.com',
      role: 'USER',
    };

    service.getProfile().subscribe((result) => expect(result).toEqual(profile));

    const request = http.expectOne(url);
    expect(request.request.method).toBe('GET');
    request.flush(profile);
  });

  it('updates the current user profile', () => {
    const update = { username: 'new-name', email: 'new@example.com' };
    const profile: UserProfile = { ...update, role: 'USER' };

    service.updateProfile(update).subscribe((result) => expect(result).toEqual(profile));

    const request = http.expectOne(url);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(update);
    request.flush(profile);
  });
});
