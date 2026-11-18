
import { Component, inject } from '@angular/core';
import { RecommendationService } from '../../core/services/recommendation.service';
import { MovieRecommendation } from '../../shared/models/movie';
import { DecimalPipe } from '@angular/common';


@Component({
  selector: 'app-recommendations',
  imports: [DecimalPipe],
  templateUrl: './recommendations.html',
  styleUrl: './recommendations.scss',
})
export class Recommendations {

  private recommendationService = inject(RecommendationService);

  movies: MovieRecommendation[] = [];
  loading = true;
  error = false;

  ngOnInit(): void {
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.loading = true;
    this.error = false;

    this.recommendationService.getRecommendations().subscribe({
      next: (data) => {
        this.movies = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load recommendations:', err);
        this.error = true;
        this.loading = false;
      }
    });
  }
}
