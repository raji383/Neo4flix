import { isPlatformBrowser } from '@angular/common';
import { Injectable } from '@angular/core';
import { inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../../shared/models/auth';



@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private readonly apiUrl = `${environment.apiUrl}/api/auth`;
    private readonly platformId = inject(PLATFORM_ID);

    private get tokenStorage(): Storage | null {
        return isPlatformBrowser(this.platformId) ? localStorage : null;
    }

    constructor(private http: HttpClient) { }

    login(credentials: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(
            `${this.apiUrl}/login`,
            credentials
        ).pipe(
            tap(response => {
                this.tokenStorage?.setItem('jwtToken', response.jwtToken);
                console.log(response);
                
            })
        );
    }
    register(data: RegisterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(
            `${this.apiUrl}/register`,
            data
        ).pipe(
            tap(response => {
                this.tokenStorage?.setItem('jwtToken', response.jwtToken);
            })
        );
    }

    logout(): void {
        this.tokenStorage?.removeItem('jwtToken');
    }

    getToken(): string | null {
        return this.tokenStorage?.getItem('jwtToken') ?? null;
    }

    isAuthenticated(): boolean {
        return !!this.getToken();
    }
}