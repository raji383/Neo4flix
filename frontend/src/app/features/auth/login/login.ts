import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);

  isLoading = signal(false);
  router = inject(Router);

  errorMessage = '';

  loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });
  showPassword = signal(false);

  togglePassword() {
    this.showPassword.update(v => !v);
  }
  onSubmit(): void {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage = '';

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (response) => {
        console.log('Login successful:', response.username);
        this.isLoading.set(false);
        this.router.navigate(['/profile']);



      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage = this.errorMessageService.getHttpErrorMessage(
          error,
          'An error occurred during login.',
        );

      }
    });
  }
}
