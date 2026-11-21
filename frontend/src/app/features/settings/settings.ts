import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { UserProfile } from '../../shared/models/user-profile.model';
import { OtpVerificationComponent } from '../../shared/components/otp-verification/otp-verification';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, OtpVerificationComponent],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly profileService = inject(UserProfileService);

  readonly settings = signal<UserProfile | null>(null);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isTwoFactorLoading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly twoFactorChallengeId = signal('');
  readonly twoFactorError = signal('');

  readonly profileForm = this.formBuilder.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.settings.set(profile);
        this.profileForm.setValue({
          username: profile.username,
          email: profile.email,
        });
        this.profileForm.markAsPristine();
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Unable to load settings.'),
        );
        this.isLoading.set(false);
      },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.isSaving()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.profileService.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (profile) => {
        this.settings.set(profile);
        this.profileForm.setValue({
          username: profile.username,
          email: profile.email,
        });
        this.profileForm.markAsPristine();
        this.successMessage.set('Profile updated successfully.');
        this.isSaving.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          this.errorMessageService.getHttpErrorMessage(
            error,
            'Unable to update your profile.',
          ),
        );
        this.isSaving.set(false);
      },
    });
  }

  startTwoFactorSetup(): void {
    if (this.isTwoFactorLoading()) return;

    this.isTwoFactorLoading.set(true);
    this.twoFactorError.set('');
    this.successMessage.set('');

    this.authService.enableTwoFactor().subscribe({
      next: (response) => {
        this.twoFactorChallengeId.set(response.challengeId);
        this.successMessage.set(response.message);
        this.isTwoFactorLoading.set(false);
      },
      error: (error) => {
        this.twoFactorError.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Could not start two-factor setup.'),
        );
        this.isTwoFactorLoading.set(false);
      },
    });
  }

  verifyTwoFactorSetup(otp: string): void {
    if (this.isTwoFactorLoading()) return;

    this.isTwoFactorLoading.set(true);
    this.twoFactorError.set('');

    this.authService.verifyEmail({
      challengeId: this.twoFactorChallengeId(),
      otp,
    }).subscribe({
      next: () => {
        this.settings.update(settings => settings ? { ...settings, twoFactorEnabled: true } : settings);
        this.twoFactorChallengeId.set('');
        this.successMessage.set('Two-factor authentication is enabled.');
        this.isTwoFactorLoading.set(false);
      },
      error: (error) => {
        this.twoFactorError.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Invalid or expired verification code.'),
        );
        this.isTwoFactorLoading.set(false);
      },
    });
  }

  disableTwoFactor(): void {
    if (this.isTwoFactorLoading()) return;

    this.isTwoFactorLoading.set(true);
    this.twoFactorError.set('');
    this.successMessage.set('');

    this.authService.disableTwoFactor().subscribe({
      next: () => {
        this.settings.update(settings => settings ? { ...settings, twoFactorEnabled: false } : settings);
        this.successMessage.set('Two-factor authentication is disabled.');
        this.isTwoFactorLoading.set(false);
      },
      error: (error) => {
        this.twoFactorError.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Could not disable two-factor authentication.'),
        );
        this.isTwoFactorLoading.set(false);
      },
    });
  }
}