import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { CreateMovie, MovieSearchParams } from '../../shared/models/movie';
import { Observable } from 'rxjs/internal/Observable';
import { PageResponse } from '../../shared/models/pageResponse';

@Injectable({
  providedIn: 'root'
})
export class Movie {
  private readonly http = inject(HttpClient);
  private readonly baseUrl: string = 'http://localhost:8080/api/movies';

  checkout(request: CreateMovie): Observable<Movie> {
    return this.http.post<Movie>(`${this.baseUrl}/add`, request);
  }
  search(params: MovieSearchParams = {}): Observable<PageResponse<Movie>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<Movie>>(this.baseUrl, { params: httpParams });
  }

  getById(id: string): Observable<Movie> {
    return this.http.get<Movie>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }
  update(id: string, request: CreateMovie): Observable<Movie> {
    return this.http.put<Movie>(`${this.baseUrl}/${encodeURIComponent(id)}`, request);
  }
}
