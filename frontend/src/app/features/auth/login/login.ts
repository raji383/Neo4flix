
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

    router = inject(Router);

    isLoading = signal(false);
    showPassword = signal(false);
    isOtpStep = signal(false);

    challengeId = signal('');
    errorMessage = signal('');

    loginForm = this.fb.nonNullable.group({
        username: ['', Validators.required],
        password: ['', Validators.required]
    });

    otpForm = this.fb.nonNullable.group({
        otp: ['', [
            Validators.required,
            Validators.pattern(/^\d{6}$/)
        ]]
    });

    togglePassword(): void {
        this.showPassword.update(value => !value);
    }

    onSubmit(): void {
        if (this.loginForm.invalid || this.isLoading()) {
            this.loginForm.markAllAsTouched();
            return;
        }

        this.isLoading.set(true);
        this.errorMessage.set('');

        this.authService.login(this.loginForm.getRawValue()).subscribe({
            next: (response) => {
                this.isLoading.set(false);

                if (response.requiresTwoFactor) {
                    this.challengeId.set(response.challengeId);
                    this.isOtpStep.set(true);
                    return;
                }

                this.errorMessage.set(
                    'Two-factor authentication is required.'
                );
            },
            error: (error) => {
                this.isLoading.set(false);

                this.errorMessage.set(
                    this.errorMessageService.getHttpErrorMessage(
                        error,
                        'An error occurred during login.'
                    )
                );
            }
        });
    }

    verifyOtp(): void {
        if (this.otpForm.invalid || this.isLoading()) {
            this.otpForm.markAllAsTouched();
            return;
        }

        this.isLoading.set(true);
        this.errorMessage.set('');

        this.authService.verifyTwoFactor({
            challengeId: this.challengeId(),
            otp: this.otpForm.controls.otp.value
        }).subscribe({
            next: (response) => {
                this.isLoading.set(false);
                console.log('Authentication successful:', response.username);
                this.router.navigate(['/profile']);
            },
            error: (error) => {
                this.isLoading.set(false);

                this.errorMessage.set(
                    this.errorMessageService.getHttpErrorMessage(
                        error,
                        'Invalid or expired verification code.'
                    )
                );
            }
        });
    }

    backToLogin(): void {
        this.isOtpStep.set(false);
        this.challengeId.set('');
        this.otpForm.reset();
        this.errorMessage.set('');
    }
}
