
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MovieRecommendation } from '../../shared/models/movie';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {

  private readonly apiUrl = `${environment.apiUrl}/api/recommendations`;

  constructor(private http: HttpClient) {}

  getRecommendations(): Observable<MovieRecommendation[]> {
    return this.http.get<MovieRecommendation[]>(this.apiUrl);
  }
}
