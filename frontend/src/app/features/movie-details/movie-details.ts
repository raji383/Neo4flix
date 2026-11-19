import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, map, of, switchMap } from 'rxjs';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { MovieService } from '../../core/services/movie.service';
import { Movie } from '../../shared/models/movie';
import { RatingComponent } from "../../shared/components/rating/rating";
import { WatchlistService } from '../../core/services/watchlist.service';
import { ShareMovie } from '../../shared/components/share-movie/share-movie';

@Component({
  imports: [RouterLink, RatingComponent,ShareMovie],
  selector: 'app-movie-details',
  styleUrl: './movie-details.scss',
  templateUrl: './movie-details.html',
})
export class MovieDetails {
  private readonly route = inject(ActivatedRoute);
  private readonly movieService = inject(MovieService);
  private readonly watchlistService = inject(WatchlistService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly destroyRef = inject(DestroyRef);

  readonly movie = signal<Movie | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageFailed = signal(false);
  readonly inWatchlist = signal<boolean | null>(null);
  readonly watchlistLoading = signal(true);
  readonly watchlistPending = signal(false);
  readonly watchlistError = signal<string | null>(null);

  constructor() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = params.get('id');
          this.loading.set(true);
          this.error.set(null);
          this.imageFailed.set(false);
          this.inWatchlist.set(null);
          this.watchlistLoading.set(true);
          this.watchlistError.set(null);

          if (!id) {
            this.error.set('Movie not found.');
            this.loading.set(false);
            this.watchlistLoading.set(false);
            return EMPTY;
          }

          return this.movieService.getById(id).pipe(
            switchMap((movie) => this.watchlistService.isInWatchlist(movie.id).pipe(
              map((response) => ({ movie, inWatchlist: response.inWatchlist })),
              catchError((error: unknown) => {
                this.watchlistError.set(
                  this.errorMessageService.getHttpErrorMessage(error, 'Could not check your watchlist.'),
                );
                return of({ movie, inWatchlist: null });
              }),
              finalize(() => this.watchlistLoading.set(false)),
            )),
            catchError((error: unknown) => {
              this.error.set(this.errorMessageService.getHttpErrorMessage(error, 'Could not load this movie.'));
              return EMPTY;
            }),
            finalize(() => {
              this.loading.set(false);
              this.watchlistLoading.set(false);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe(({ movie, inWatchlist }) => {
        this.movie.set(movie);
        this.inWatchlist.set(inWatchlist);
      });
  }

  toggleWatchlist(movieId: string): void {
    const wasInWatchlist = this.inWatchlist();
    if (wasInWatchlist === null || this.watchlistLoading() || this.watchlistPending()) {
      return;
    }

    this.watchlistError.set(null);
    this.watchlistPending.set(true);
    const request = wasInWatchlist
      ? this.watchlistService.delete(movieId)
      : this.watchlistService.create(movieId);

    request.pipe(finalize(() => this.watchlistPending.set(false))).subscribe({
      next: () => this.inWatchlist.set(!wasInWatchlist),
      error: (error: unknown) => {
        this.watchlistError.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Could not update your watchlist.'),
        );
      },
    });
  }

  stars(rating: number | null | undefined): ('full' | 'half' | 'empty')[] {
    const value = rating ?? 0;
    return Array.from({ length: 5 }, (_, i) =>
      value >= i + 1 ? 'full' : value >= i + 0.5 ? 'half' : 'empty',
    );
  }

  formatRating(rating: number | null | undefined): string {
    return rating == null ? '—' : rating.toFixed(1);
  }

  displayRating(movie: Movie): string {
    return this.formatRating(movie.averageRating);
  }

  ratingForStars(movie: Movie): number | null {
    return movie.averageRating ?? null;
  }

  ratingAriaLabel(movie: Movie): string {
    if (movie.averageRating != null) {
      return `${this.formatRating(movie.averageRating)} out of 5 from ${movie.ratingCount ?? 0} app ratings`;
    }
    return 'No ratings yet';
  }

  ratingCountLabel(movie: Movie): string {
    if (movie.averageRating != null) {
      return `${movie.ratingCount ?? 0} app ratings`;
    }
    return 'No ratings yet';
  }
  onRatingChanged(): void {
    const currentMovie = this.movie();

    if (!currentMovie) {
      return;
    }

    this.movieService.getById(currentMovie.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (movie) => this.movie.set(movie),
        error: (error: unknown) => {
          this.error.set(
            this.errorMessageService.getHttpErrorMessage(
              error,
              'Could not refresh movie details.',
            ),
          );
        },
      });
  }

}
