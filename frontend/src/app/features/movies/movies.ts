import { Component, computed, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { MovieService } from '../../core/services/movie.service';
import { Movie, MovieSearchParams } from '../../shared/models/movie';
import { PageResponse } from '../../shared/models/pageResponse';
import { WatchlistService } from '../../core/services/watchlist.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  imports: [RouterLink],
  selector: 'app-movies',
  styleUrl: './movies.scss',
  templateUrl: './movies.html',
})
export class Movies {
  private readonly watchlistService = inject(WatchlistService);

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly movieService = inject(MovieService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly notifications = inject(NotificationService);

  readonly movies = signal<Movie[]>([]);
  readonly popular = signal<Movie[]>([]);
  readonly genreList = signal<string[]>([]);
  readonly selectedGenres = signal<string[]>([]);
  readonly selectedReleaseYear = signal('');
  readonly searchQuery = signal('');
  readonly pageResponse = signal<PageResponse<Movie> | null>(null);
  readonly filterParams = signal<MovieSearchParams>({ page: 0, size: 12, genres: [] });
  readonly requestParams = computed<MovieSearchParams>(() => ({
    ...this.filterParams(),
    q: this.searchQuery(),
  }));
  readonly featuredId = signal<string | null>(null);
  readonly failedImages = signal<Set<string>>(new Set());
  readonly loadingMovies = signal(true);
  readonly loadingPopular = signal(true);
  readonly moviesError = signal<string | null>(null);
  readonly popularError = signal<string | null>(null);
  readonly genresError = signal<string | null>(null);
  readonly watchlistMovieIds = signal<ReadonlySet<string>>(new Set());
  readonly pendingWatchlistIds = signal<ReadonlySet<string>>(new Set());
  readonly watchlistLoading = signal(true);
  readonly watchlistError = signal<string | null>(null);
  readonly featured = computed(() => {
    const movies = [...this.popular(), ...this.movies()];
    return movies.find((movie) => movie.id === this.featuredId()) ?? movies[0] ?? null;
  });

  constructor() {
    this.watchlistService
      .getWatchlist()
      .pipe(
        catchError((error: unknown) => {
          this.watchlistError.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not load your watchlist.'),
          );
          return EMPTY;
        }),
        finalize(() => this.watchlistLoading.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe((items) => this.watchlistMovieIds.set(new Set(items.map((item) => item.id))));

    toObservable(this.requestParams)
      .pipe(
        switchMap((params) => {
          this.loadingMovies.set(true);
          this.moviesError.set(null);

          return this.movieService.search(params).pipe(
            catchError((error: unknown) => {
              this.moviesError.set(
                this.errorMessageService.getHttpErrorMessage(error, 'Could not load movies.'),
              );
              return EMPTY;
            }),
            finalize(() => this.loadingMovies.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((response) => {
        this.pageResponse.set(response);
        this.movies.set(response.content);
      });

    this.movieService
      .getPopular(20)
      .pipe(
        catchError((error: unknown) => {
          this.popularError.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not load popular movies.'),
          );
          return EMPTY;
        }),
        finalize(() => this.loadingPopular.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe((movies) => {
        this.popular.set(movies);
        this.featuredId.set(movies[0]?.id ?? null);
      });

    this.movieService
      .getGenres()
      .pipe(
        catchError((error: unknown) => {
          this.genresError.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not load movie genres.'),
          );
          return EMPTY;
        }),
        takeUntilDestroyed(),
      )
      .subscribe((genres) => this.genreList.set(genres));
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.filterParams.update((params) => ({ ...params, page: 0 }));
  }

  onReleaseYear(value: string) {
    this.selectedReleaseYear.set(value);
  }

  applyFilters() {
    const releaseYear = this.selectedReleaseYear()
      ? Number(this.selectedReleaseYear())
      : undefined;
    const validReleaseYear =
      releaseYear !== undefined &&
        Number.isInteger(releaseYear) &&
        releaseYear >= 1888
        ? releaseYear
        : undefined;

    this.filterParams.update((params) => ({
      ...params,
      genres: [...this.selectedGenres()],
      releaseYear: validReleaseYear,
      page: 0,
    }));
  }

  selectMovie(movieId: string) {
    this.featuredId.set(movieId);
    const scrollContainer = this.host.nativeElement.closest('.content');
    if (scrollContainer instanceof HTMLElement) {
      scrollContainer.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  toggleGenre(genre: string) {
    this.selectedGenres.update((genres) =>
      genres.includes(genre) ? genres.filter((value) => value !== genre) : [...genres, genre],
    );
  }

  isGenreActive(genre: string) {
    return this.selectedGenres().includes(genre);
  }

  previousPage() {
    const response = this.pageResponse();
    if (response && response.page > 0) {
      this.filterParams.update((params) => ({ ...params, page: response.page - 1 }));
    }
  }

  nextPage() {
    const response = this.pageResponse();
    if (response && response.page + 1 < response.totalPages) {
      this.filterParams.update((params) => ({ ...params, page: response.page + 1 }));
    }
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

  onImageError(id: string) {
    this.failedImages.update((images) => new Set(images).add(id));
  }

  hasImage(id: string) {
    return !this.failedImages().has(id);
  }

  scrollCarousel(el: HTMLElement, direction: -1 | 1) {
    el.scrollBy({ left: el.clientWidth * 0.8 * direction, behavior: 'smooth' });
  }

  isInWatchlist(movieId: string): boolean {
    return this.watchlistMovieIds().has(movieId);
  }

  isWatchlistPending(movieId: string): boolean {
    return this.pendingWatchlistIds().has(movieId);
  }

  toggleWatchlist(movieId: string): void {
    if (this.watchlistLoading() || this.isWatchlistPending(movieId)) {
      return;
    }

    this.watchlistError.set(null);
    this.pendingWatchlistIds.update((ids) => new Set(ids).add(movieId));
    const wasInWatchlist = this.isInWatchlist(movieId);
    const request = wasInWatchlist
      ? this.watchlistService.delete(movieId)
      : this.watchlistService.create(movieId);

    request.pipe(finalize(() => {
      this.pendingWatchlistIds.update((ids) => {
        const next = new Set(ids);
        next.delete(movieId);
        return next;
      });
    })).subscribe({
      next: () => {
        this.watchlistMovieIds.update((ids) => {
          const next = new Set(ids);
          if (wasInWatchlist) {
            next.delete(movieId);
          } else {
            next.add(movieId);
          }
          return next;
        });
        this.notifications.showSuccess(
          wasInWatchlist ? 'Removed from your watchlist.' : 'Added to your watchlist.',
        );
      },
      error: (error: unknown) => {
        this.notifications.showHttpError(error, 'Could not update your watchlist.');
      },
    });
  }
}