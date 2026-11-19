import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize } from 'rxjs';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { WatchlistService } from '../../core/services/watchlist.service';
import { WatchlistItem } from '../../shared/models/whatchlist';

@Component({
  imports: [RouterLink],
  selector: 'app-watchlist',
  styleUrl: './watchlist.scss',
  templateUrl: './watchlist.html',
})
export class Watchlist {
  private readonly watchlistService = inject(WatchlistService);
  private readonly errorMessageService = inject(ErrorMessageService);

  readonly movies = signal<WatchlistItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly removingIds = signal<ReadonlySet<string>>(new Set());

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
