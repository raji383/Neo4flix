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
  };

  beforeEach(async () => {
    authService.register.mockReset();

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

  it('registers the account and continues to login without email verification', () => {
    authService.register.mockReturnValue(of({
      id: 'user-id',
      username: 'filmfan',
      email: 'filmfan@example.com',
      role: 'USER',
      message: 'Registration successful.',
    }));
    const navigate = vi.spyOn(component.router, 'navigate');
    component.registerForm.setValue({
      username: 'filmfan',
      email: 'filmfan@example.com',
      password: 'StrongPass1!',
      confirmPassword: 'StrongPass1!',
    });

    component.onSubmit();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
