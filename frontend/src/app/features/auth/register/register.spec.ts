import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { REGISTER } from './register';

describe('REGISTER', () => {
  let component: REGISTER;
  let fixture: ComponentFixture<REGISTER>;
  const authService = {
    register: vi.fn(),
    verifyEmail: vi.fn(),
  };

  beforeEach(async () => {
    authService.register.mockReset();
    authService.verifyEmail.mockReset();

    await TestBed.configureTestingModule({
      imports: [REGISTER],
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(REGISTER);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('moves to email verification after registration returns a challenge', () => {
    authService.register.mockReturnValue(of({
      id: 'user-id',
      username: 'filmfan',
      email: 'filmfan@example.com',
      role: 'USER',
      message: 'Verify your email',
      challengeId: 'email-challenge',
    }));
    component.registerForm.setValue({
      username: 'filmfan',
      email: 'filmfan@example.com',
      password: 'StrongPass1!',
      confirmPassword: 'StrongPass1!',
    });

    component.onSubmit();

    expect(component.isEmailVerificationStep()).toBe(true);
    expect(component.challengeId()).toBe('email-challenge');
    expect(component.registeredEmail()).toBe('filmfan@example.com');
  });

  it('verifies the email challenge before showing the sign-in state', () => {
    authService.verifyEmail.mockReturnValue(of(void 0));
    component.challengeId.set('email-challenge');
    component.verificationForm.controls.otp.setValue('123456');

    component.verifyEmail();

    expect(authService.verifyEmail).toHaveBeenCalledWith({
      challengeId: 'email-challenge',
      otp: '123456',
    });
    expect(component.emailVerified()).toBe(true);
  });
});
