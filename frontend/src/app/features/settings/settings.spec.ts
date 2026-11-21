import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { Settings } from './settings';

describe('Settings', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;
  const profileService = {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  };
  const authService = {
    enableTwoFactor: vi.fn(),
    verifyEmail: vi.fn(),
    disableTwoFactor: vi.fn(),
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
    authService.verifyEmail.mockReset();
    authService.disableTwoFactor.mockReset();

    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: UserProfileService, useValue: profileService },
        { provide: AuthService, useValue: authService },
        { provide: ErrorMessageService, useValue: errorMessageService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('updates profile details from settings', () => {
    profileService.updateProfile.mockReturnValue(of({
      id: 'user-id',
      username: 'newname',
      email: 'filmfan@example.com',
      role: 'USER',
      twoFactorEnabled: false,
    }));
    component.profileForm.controls.username.setValue('newname');

    component.saveProfile();

    expect(profileService.updateProfile).toHaveBeenCalledWith({
      username: 'newname',
      email: 'filmfan@example.com',
    });
    expect(component.settings()?.username).toBe('newname');
  });

  it('requires email verification before enabling 2FA and supports disabling it', () => {
    authService.enableTwoFactor.mockReturnValue(of({
      challengeId: 'activation-challenge',
      message: 'A verification code has been sent to your email.',
    }));
    authService.verifyEmail.mockReturnValue(of(void 0));
    authService.disableTwoFactor.mockReturnValue(of(void 0));

    component.startTwoFactorSetup();
    expect(component.twoFactorChallengeId()).toBe('activation-challenge');

    component.verifyTwoFactorSetup('123456');
    expect(authService.verifyEmail).toHaveBeenCalledWith({
      challengeId: 'activation-challenge',
      otp: '123456',
    });
    expect(component.settings()?.twoFactorEnabled).toBe(true);

    component.disableTwoFactor();
    expect(component.settings()?.twoFactorEnabled).toBe(false);
  });
});