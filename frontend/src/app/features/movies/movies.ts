import { Component, computed, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { MovieService } from '../../core/services/movie.service';
import { Movie, MovieSearchParams } from '../../shared/models/movie';
import { PageResponse } from '../../shared/models/pageResponse';

@Component({
  imports: [RouterLink],
  selector: 'app-movies',
  styleUrl: './movies.scss',
  templateUrl: './movies.html',
})
export class Movies {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly movieService = inject(MovieService);
  private readonly errorMessageService = inject(ErrorMessageService);

  readonly movies = signal<Movie[]>([]);
  readonly popular = signal<Movie[]>([]);
  readonly genreList = signal<string[]>([]);
  readonly pageResponse = signal<PageResponse<Movie> | null>(null);
  readonly params = signal<MovieSearchParams>({ q: '', page: 0, size: 12, genres: [] });
  readonly featuredId = signal<string | null>(null);
  readonly failedImages = signal<Set<string>>(new Set());
  readonly loadingMovies = signal(true);
  readonly loadingPopular = signal(true);
  readonly moviesError = signal<string | null>(null);
  readonly popularError = signal<string | null>(null);
  readonly genresError = signal<string | null>(null);

  readonly featured = computed(() => {
    const movies = [...this.popular(), ...this.movies()];
    return movies.find((movie) => movie.id === this.featuredId()) ?? movies[0] ?? null;
  });

  constructor() {
    toObservable(this.params)
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
      .getPopular(8)
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
    this.params.update((params) => ({ ...params, q: value, page: 0 }));
  }

  onReleaseYear(value: string) {
    const releaseYear = value ? Number(value) : undefined;
    const validReleaseYear =
      releaseYear !== undefined &&
      Number.isInteger(releaseYear) &&
      releaseYear >= 1888 &&
      releaseYear <= 2100
        ? releaseYear
        : undefined;
    this.params.update((params) => ({
      ...params,
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
    this.params.update((params) => {
      const current = params.genres ?? [];
      const genres = current.includes(genre)
        ? current.filter((value) => value !== genre)
        : [...current, genre];
      return { ...params, genres, page: 0 };
    });
  }

  isGenreActive(genre: string) {
    return this.params().genres?.includes(genre) ?? false;
  }

  previousPage() {
    const response = this.pageResponse();
    if (response && response.page > 0) {
      this.params.update((params) => ({ ...params, page: response.page - 1 }));
    }
  }

  nextPage() {
    const response = this.pageResponse();
    if (response && response.page + 1 < response.totalPages) {
      this.params.update((params) => ({ ...params, page: response.page + 1 }));
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

  onImageError(id: string) {
    this.failedImages.update((images) => new Set(images).add(id));
  }

  hasImage(id: string) {
    return !this.failedImages().has(id);
  }

  scrollCarousel(el: HTMLElement) {
    el.scrollBy({ left: el.clientWidth * 0.8, behavior: 'smooth' });
  }

}
