
import { Component, inject, signal, Signal } from '@angular/core';
import { RecommendationService } from '../../core/services/recommendation.service';
import { MovieRecommendation } from '../../shared/models/movie';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from "@angular/router";


@Component({
  selector: 'app-recommendations',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './recommendations.html',
  styleUrl: './recommendations.scss',
})
export class Recommendations {

  private recommendationService = inject(RecommendationService);

  movies = signal<MovieRecommendation[]>([]);
  loading = signal(true);
  error = signal(false);

  ngOnInit(): void {
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.loading.set(true);
    this.error.set(false);

    this.recommendationService.getRecommendations().subscribe({
      next: (data) => {
        this.movies.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load recommendations:', err);
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }
}
