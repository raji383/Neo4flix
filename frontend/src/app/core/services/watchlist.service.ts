import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WatchlistItem } from '../../shared/models/whatchlist';


@Injectable({
    providedIn: 'root'
})
export class WatchlistService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/watchlist`;

    getWatchlist(): Observable<WatchlistItem[]> {
        return this.http.get<WatchlistItem[]>(this.baseUrl);
    }

    isInWatchlist(id: string): Observable<{"inWatchlist": boolean}> {
        return this.http.get<{"inWatchlist": boolean}>(`${this.baseUrl}/${encodeURIComponent(id)}`);
    }

    create(movieId: string): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${encodeURIComponent(movieId)}`, null);
    }

    delete(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(id)}`);
    }


}
