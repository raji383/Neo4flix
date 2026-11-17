import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateMovie, Movie, MovieSearchParams } from '../../shared/models/movie';
import { PageResponse } from '../../shared/models/pageResponse';

@Injectable({
  providedIn: 'root',
})
export class MovieService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/movies`;

  search(params: MovieSearchParams = {}): Observable<PageResponse<Movie>> {
    let httpParams = new HttpParams();
    const { genres, ...filters } = params;

    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }

    for (const genre of genres ?? []) {
      httpParams = httpParams.append('genre', genre);
    }

    return this.http.get<PageResponse<Movie>>(this.baseUrl, { params: httpParams });
  }

  getPopular(limit = 8): Observable<Movie[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<Movie[]>(`${this.baseUrl}/popular`, { params });
  }

  getGenres(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/api/genres`);
  }

  getById(id: string): Observable<Movie> {
    return this.http.get<Movie>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }

  create(request: CreateMovie): Observable<Movie> {
    return this.http.post<Movie>(`${this.baseUrl}/add`, this.toFormData(request));
  }

  update(id: string, request: CreateMovie): Observable<Movie> {
    return this.http.put<Movie>(`${this.baseUrl}/${encodeURIComponent(id)}`, this.toFormData(request));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }

  private toFormData(request: CreateMovie): FormData {
    const formData = new FormData();
    formData.set('title', request.title);
    formData.set('director', request.director);
    formData.set('description', request.description);
    formData.set('releaseYear', String(request.releaseYear));
    request.genres.forEach((genre) => formData.append('genres', genre));
    if (request.file) {
      formData.set('file', request.file);
    }
    return formData;
  }
}
