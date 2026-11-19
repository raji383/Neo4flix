
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { EMPTY, catchError, finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { RatingService } from '../../core/services/rating.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { RatedMovie } from '../../shared/models/rating';

type SortBy = 'rating-desc' | 'rating-asc' | 'title' | 'year';

@Component({
  selector: 'app-ratings',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './ratings.html',
  styleUrl: './ratings.scss',
})
export class Ratings {
  private readonly ratingService = inject(RatingService);
  private readonly errorMessageService = inject(ErrorMessageService);

  readonly stars = [1, 2, 3, 4, 5];

  readonly isLoading = signal(true);
  readonly movies = signal<RatedMovie[]>([]);
  readonly error = signal<string | null>(null);

  readonly search = signal('');
  readonly sortBy = signal<SortBy>('rating-desc');

  constructor() {
    this.loadRatings();
  }

  private loadRatings(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.ratingService
      .getMyRatings()
      .pipe(
        catchError((error: unknown) => {
          this.error.set(
            this.errorMessageService.getHttpErrorMessage(
              error,
              'Could not load your rating history.',
            ),
          );

          return EMPTY;
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(),
      )
      .subscribe((ratings) => {
        console.log(ratings);
        
        this.movies.set(ratings);
      });
  }

  retry(): void {
    this.loadRatings();
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.search.set(input.value);
  }

  onSortChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const value = select.value;

    const validSorts: SortBy[] = [
      'rating-desc',
      'rating-asc',
      'title',
      'year',
    ];

    if (validSorts.includes(value as SortBy)) {
      this.sortBy.set(value as SortBy);
    }
  }

  readonly myAverage = computed(() => {
    const list = this.movies();

    if (!list.length) return '–';

    const average =
      list.reduce((sum, movie) => sum + movie.myRating, 0) /
      list.length;

    return average.toFixed(1);
  });

  readonly visibleMovies = computed(() => {
    const term = this.search().trim().toLowerCase();

    const filtered = this.movies().filter((movie) =>
      movie.title.toLowerCase().includes(term),
    );

    return [...filtered].sort((a, b) => {
      switch (this.sortBy()) {
        case 'rating-asc':
          return a.myRating - b.myRating;

        case 'title':
          return a.title.localeCompare(b.title);

        case 'year':
          return b.releaseYear - a.releaseYear;

        default:
          return b.myRating - a.myRating;
      }
    });
  });

  /** Same title always gives the same color */
  colorFor(title: string): string {
    let hash = 0;

    for (const ch of title) {
      hash = ch.charCodeAt(0) + ((hash << 5) - hash);
    }

    const hue = Math.abs(hash) % 360;

    return `linear-gradient(
      135deg,
      hsl(${hue}, 65%, 45%),
      hsl(${(hue + 50) % 360}, 65%, 28%)
    )`;
  }
}
