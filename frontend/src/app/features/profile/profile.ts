import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { UserProfile } from '../../shared/models/user-profile.model';
import { WatchlistItem } from '../../shared/models/whatchlist';
import { WatchlistService } from '../../core/services/watchlist.service';
import { NotificationService } from '../../core/services/notification.service';
import { catchError } from 'rxjs/internal/operators/catchError';
import { EMPTY } from 'rxjs/internal/observable/empty';
import { finalize } from 'rxjs/internal/operators/finalize';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private readonly watchlistService = inject(WatchlistService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly notifications = inject(NotificationService);

  readonly movies = signal<WatchlistItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly removingIds = signal<ReadonlySet<string>>(new Set());
  private profileService = inject(UserProfileService);
  private authService = inject(AuthService);
  private router = inject(Router);

  isLoading = signal(true);
  errorMessage = signal('');
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
        next: () => {
          this.movies.update((movies) => movies.filter((movie) => movie.id !== movieId));
          this.notifications.showSuccess('Movie removed from your watchlist.');
        },
        error: (error: unknown) => {
          this.notifications.showHttpError(error, 'Could not remove this movie.');
        },
      });
  }

  isRemoving(movieId: string): boolean {
    return this.removingIds().has(movieId);
  }
}
