
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RatedMovie } from '../../shared/models/rating';

@Injectable({
  providedIn: 'root',
})
export class RatingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/ratings`;

  getMyRatings(): Observable<RatedMovie[]> {
    return this.http.get<RatedMovie[]>(`${this.baseUrl}/me`);
  }
}
