import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { UserProfile } from '../../shared/models/user-profile.model';
import { WatchlistItem } from '../../shared/models/whatchlist';
import { WatchlistService } from '../../core/services/watchlist.service';
import { OtpVerificationComponent } from '../../shared/components/otp-verification/otp-verification';
import { catchError } from 'rxjs/internal/operators/catchError';
import { EMPTY } from 'rxjs/internal/observable/empty';
import { finalize } from 'rxjs/internal/operators/finalize';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, OtpVerificationComponent],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private readonly watchlistService = inject(WatchlistService);
  private readonly errorMessageService = inject(ErrorMessageService);

  readonly movies = signal<WatchlistItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly removingIds = signal<ReadonlySet<string>>(new Set());
  private fb = inject(FormBuilder);
  private profileService = inject(UserProfileService);
  private authService = inject(AuthService);
  private router = inject(Router);

  isLoading = signal(true);
  isSaving = signal(false);
  isEditing = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  twoFactorChallengeId = signal('');
  twoFactorError = signal('');
  isTwoFactorLoading = signal(false);

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
        this.user.update(user => user ? { ...user, twoFactorEnabled: true } : user);
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
        this.user.update(user => user ? { ...user, twoFactorEnabled: false } : user);
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

  onLogout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }



  constructor() {
    this.loadWatchlist();
  }

  loadWatchlist(): void {
    this.loading.set(true);
    this.error.set(null);
    this.watchlistService
      .getWatchlist()
      .pipe(
        catchError((error: unknown) => {
          this.error.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not load your watchlist.'),
          );
          return EMPTY;
        }),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe((movies) => this.movies.set(movies));
  }

  remove(movieId: string): void {
    if (this.removingIds().has(movieId)) {
      return;
    }

    this.error.set(null);
    this.removingIds.update((ids) => new Set(ids).add(movieId));
    this.watchlistService
      .delete(movieId)
      .pipe(finalize(() => {
        this.removingIds.update((ids) => {
          const next = new Set(ids);
          next.delete(movieId);
          return next;
        });
      }))
      .subscribe({
        next: () => this.movies.update((movies) => movies.filter((movie) => movie.id !== movieId)),
        error: (error: unknown) => {
          this.error.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not remove this movie.'),
          );
        },
      });
  }

  isRemoving(movieId: string): boolean {
    return this.removingIds().has(movieId);
  }
}
