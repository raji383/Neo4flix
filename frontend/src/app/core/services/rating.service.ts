
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RatedMovie, RatingRequest } from '../../shared/models/rating';

@Injectable({
  providedIn: 'root',
})
export class RatingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/ratings`;

  getMyRatings(): Observable<RatedMovie[]> {
    return this.http.get<RatedMovie[]>(`${this.baseUrl}/me`);
  }
  getMyRatingToMovie(movieId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/${movieId}`);
  }

  addRating(movieId: string, value: number): Observable<void> {
    const request: RatingRequest = { value };

    return this.http.post<void>(
      `${this.baseUrl}/${movieId}`,
      request,
    );
  }

  updateRating(movieId: string, value: number): Observable<void> {
    const request: RatingRequest = { value };

    return this.http.put<void>(
      `${this.baseUrl}/${movieId}`,
      request,
    );
  }

  deleteRating(movieId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${movieId}`);
  }
}
