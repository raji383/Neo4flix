
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { OtpVerificationComponent } from '../../../shared/components/otp-verification/otp-verification';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink, OtpVerificationComponent],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})
export class Login {

    private fb = inject(FormBuilder);
    private authService = inject(AuthService);
    private notifications = inject(NotificationService);

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
                    this.challengeId.set(response.challengeId ?? '');
                    this.isOtpStep.set(true);
                    return;
                }

                if (response.jwtToken) {
                    void this.router.navigate(['/movies']);
                    return;
                }

            },
            error: (error) => {
                this.isLoading.set(false);
                this.notifications.showHttpError(error, 'An error occurred during login.');
            }
        });
    }

    verifyOtp(otp: string): void {
        if (this.isLoading()) return;
        this.isLoading.set(true);
        this.errorMessage.set('');

        this.authService.verifyTwoFactor({
            challengeId: this.challengeId(),
            otp
        }).subscribe({
            next: (response) => {
                this.isLoading.set(false);
                this.router.navigate(['/profile']);
                this.notifications.showSuccess(`Welcome back ${response.username}!`);

            },
            error: (error) => {
                this.isLoading.set(false);

                this.notifications.showHttpError(
                    error,
                    'Invalid or expired verification code.'
                );
            }
        });
    }

    backToLogin(): void {
        this.isOtpStep.set(false);
        this.challengeId.set('');
        this.errorMessage.set('');
    }
}
