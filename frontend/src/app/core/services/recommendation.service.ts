
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovieRecommendation } from '../../shared/models/movie';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {

  private readonly apiUrl = '/api/recommendations';

  constructor(private http: HttpClient) {}

  getRecommendations(): Observable<MovieRecommendation[]> {
    return this.http.get<MovieRecommendation[]>(this.apiUrl);
  }
}
