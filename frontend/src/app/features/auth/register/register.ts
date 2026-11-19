import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';

const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,72}$/;

const passwordMatchValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  return password === confirmPassword
    ? null
    : { passwordMismatch: true };
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class REGISTER {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);

  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  router = inject(Router);

  registerForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.pattern(strongPasswordPattern)]],
    confirmPassword: ['', Validators.required]
  }, {
    validators: passwordMatchValidator
  });
  showPassword = signal(false);
  showConfirm = signal(false);

  passwordStrength(): number {
    const v = this.registerForm.controls.password.value ?? '';
    let score = 0;
    if (v.length >= 8) score++;
    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++;
    if (/\d/.test(v)) score++;
    if (/[^A-Za-z0-9]/.test(v)) score++;
    return score;
  }

  strengthLabel(): string {
    return ['', 'Weak', 'Fair', 'Good', 'Strong'][this.passwordStrength()];
  }
  onSubmit(): void {
    if (this.registerForm.invalid || this.isLoading()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { username, email, password, confirmPassword } =
      this.registerForm.getRawValue();

    this.authService.register({
      username,
      email,
      password,
      confirmPassword,
    }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.successMessage.set(
          `Account created successfully. Welcome ${response.username}!`
        );
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          this.errorMessageService.getHttpErrorMessage(
            error,
            error.status === 409
              ? 'Username or email is already registered.'
              : error.status === 400
                ? 'Please check the information you entered.'
                : 'Something went wrong. Please try again.',
          ),
        );
      }
    });
  }
}
