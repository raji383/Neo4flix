
import { Component, inject, signal, Signal } from '@angular/core';
import { RecommendationService } from '../../core/services/recommendation.service';
import { MovieRecommendation } from '../../shared/models/movie';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from "@angular/router";
import { ErrorMessageService } from '../../core/services/error-message.service';


@Component({
  selector: 'app-recommendations',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './recommendations.html',
  styleUrl: './recommendations.scss',
})
export class Recommendations {

  private recommendationService = inject(RecommendationService);
  private readonly errorMessageService = inject(ErrorMessageService);

  movies = signal<MovieRecommendation[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.loading.set(true);
    this.error.set(null);

    this.recommendationService.getRecommendations().subscribe({
      next: (data) => {
        this.movies.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Could not load your recommendations.'),
        );
        this.loading.set(false);
      }
    });
  }
}
