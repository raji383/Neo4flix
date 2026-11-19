import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { RatingService } from '../../../core/services/rating.service';

@Component({
  selector: 'app-rating',
  standalone: true,
  templateUrl: './rating.html',
  styleUrl: './rating.scss',
})
export class RatingComponent {
  private ratingService = inject(RatingService);

  movieId = input.required<string>();
  ratingChanged = output<void>();

  stars = [1, 2, 3, 4, 5];
  labels = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent'];
  myRating = signal(0);
  hover = signal(0);
  isLoading = signal(false);
  isSaving = signal(false);
  errorMessage = signal('');
  hasRating = signal(false);

  constructor() {
    // reload whenever the movie changes
    effect(() => {
      this.loadMyRating(this.movieId());
    });
  }

  private loadMyRating(id: string) {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.ratingService.getMyRatingToMovie(id).subscribe({
      next: value => {
        this.myRating.set(value ?? 0);
        this.hasRating.set(true);
        this.isLoading.set(false);
      },
      error: () => {
        this.myRating.set(0);
        this.hasRating.set(false);

        this.isLoading.set(false);
      },
    });
  }

  rate(value: number) {
    if (this.isSaving()) return;

    const previous = this.myRating();
    this.myRating.set(value); // optimistic update
    this.isSaving.set(true);
    this.errorMessage.set('');

    this.saveRating(value, previous, this.hasRating());
  }

  private saveRating(value: number, previous: number, update: boolean): void {
    const request = update
      ? this.ratingService.updateRating(this.movieId(), value)
      : this.ratingService.addRating(this.movieId(), value);

    request.subscribe({
      next: () => {
        this.hasRating.set(true);
        this.isSaving.set(false);
        this.ratingChanged.emit();
      },
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : undefined;

        
        if (!update && status === 409) {
          this.saveRating(value, previous, true);
          return;
        }

        if (update && status === 404) {
          this.saveRating(value, previous, false);
          return;
        }

        this.myRating.set(previous); // rollback
        this.isSaving.set(false);
        this.errorMessage.set('Could not save your rating. Please try again.');
      },
    });
  }
  remove() {
    if (this.isSaving()) return;

    const previous = this.myRating();
    this.myRating.set(0);
    this.isSaving.set(true);
    this.errorMessage.set('');

    this.ratingService.deleteRating(this.movieId()).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.ratingChanged.emit();
        this.hasRating.set(false);

      },
      error: () => {
        this.myRating.set(previous);
        this.isSaving.set(false);
        this.errorMessage.set('Could not remove your rating. Please try again.');
      },
    });
  }
}
