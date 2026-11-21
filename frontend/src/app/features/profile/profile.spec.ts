import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { WatchlistService } from '../../core/services/watchlist.service';
import { Profile } from './profile';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  const profileService = {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  };
  const authService = {
    enableTwoFactor: vi.fn(),
    disableTwoFactor: vi.fn(),
    verifyEmail: vi.fn(),
    logout: vi.fn(),
  };
  const watchlistService = {
    getWatchlist: vi.fn(),
    delete: vi.fn(),
  };
  const errorMessageService = {
    getHttpErrorMessage: vi.fn(() => 'Request failed'),
  };

  beforeEach(async () => {
    profileService.getProfile.mockReset().mockReturnValue(of({
      id: 'user-id',
      username: 'filmfan',
      email: 'filmfan@example.com',
      role: 'USER',
      twoFactorEnabled: false,
    }));
    profileService.updateProfile.mockReset();
    authService.enableTwoFactor.mockReset();
    authService.disableTwoFactor.mockReset();
    authService.verifyEmail.mockReset();
    authService.logout.mockReset();
    watchlistService.getWatchlist.mockReset().mockReturnValue(of([]));
    watchlistService.delete.mockReset();

    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        provideRouter([]),
        { provide: UserProfileService, useValue: profileService },
        { provide: AuthService, useValue: authService },
        { provide: WatchlistService, useValue: watchlistService },
        { provide: ErrorMessageService, useValue: errorMessageService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('starts email verification before enabling two-factor authentication', () => {
    authService.enableTwoFactor.mockReturnValue(of({
      challengeId: 'activation-challenge',
      message: 'A verification code has been sent to your email.',
    }));

    component.startTwoFactorSetup();

    expect(component.twoFactorChallengeId()).toBe('activation-challenge');
    expect(component.user()?.twoFactorEnabled).toBe(false);
  });

  it('enables two-factor authentication after verification and can disable it', () => {
    authService.verifyEmail.mockReturnValue(of(void 0));
    authService.disableTwoFactor.mockReturnValue(of(void 0));
    component.twoFactorChallengeId.set('activation-challenge');

    component.verifyTwoFactorSetup('123456');

    expect(authService.verifyEmail).toHaveBeenCalledWith({
      challengeId: 'activation-challenge',
      otp: '123456',
    });
    expect(component.user()?.twoFactorEnabled).toBe(true);

    component.disableTwoFactor();

    expect(component.user()?.twoFactorEnabled).toBe(false);
  });
});
