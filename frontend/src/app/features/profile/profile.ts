import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { UserProfile } from '../../shared/models/user-profile.model';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {

  private fb = inject(FormBuilder);
  private profileService = inject(UserProfileService);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);
  private router = inject(Router);

  isLoading = signal(true);
  isSaving = signal(false);
  isEditing = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  profileForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]]
  });
  user = signal<UserProfile | null>(null);

  /** "john_doe" -> "JD", "alice" -> "AL" */
  initials = computed(() => {
    const name = this.user()?.username ?? '';
    const parts = name.split(/[\s._-]+/).filter(Boolean);
    const letters =
      parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2);
    return letters.toUpperCase();
  });

  /** Same username always gives the same color */
  avatarGradient = computed(() => {
    const name = this.user()?.username ?? '';
    let hash = 0;
    for (const ch of name) {
      hash = ch.charCodeAt(0) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash) % 360;
    return `linear-gradient(135deg, hsl(${hue}, 70%, 50%), hsl(${(hue + 50) % 360}, 70%, 35%))`;
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.user.set(profile);
        this.profileForm.setValue({
          username: profile.username,
          email: profile.email
        });
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Unable to load your profile.'),
        );
      }
    });
  }

  onEdit(): void {
    const user = this.user();
    if (!user) return;

    this.profileForm.setValue({
      username: user.username,
      email: user.email
    });
    this.errorMessage.set('');
    this.successMessage.set('');
    this.isEditing.set(true);
  }

  onCancelEdit(): void {
    const user = this.user();
    if (user) {
      this.profileForm.setValue({
        username: user.username,
        email: user.email
      });
    }
    this.isEditing.set(false);
    this.errorMessage.set('');
  }

  onSubmit(): void {
    if (this.profileForm.invalid || this.isSaving()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const { username, email } = this.profileForm.getRawValue();

    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.profileService.updateProfile({ username, email }).subscribe({
      next: (updatedProfile) => {
        this.user.set(updatedProfile);
        this.profileForm.setValue({
          username: updatedProfile.username,
          email: updatedProfile.email
        });
        this.isSaving.set(false);
        this.isEditing.set(false);
        this.successMessage.set('Profile updated successfully!');
        this.profileForm.markAsPristine();
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(
          this.errorMessageService.getHttpErrorMessage(
            error,
            error.status === 409
              ? 'Username or email is already in use.'
              : 'Unable to update your profile.',
          ),
        );
      }
    });
  }

  onLogout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
