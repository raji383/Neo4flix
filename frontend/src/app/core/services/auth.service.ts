import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../../shared/models/auth';



@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private readonly apiUrl = `${environment.apiUrl}/api/auth`;

    constructor(private http: HttpClient) { }

    login(credentials: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(
            `${this.apiUrl}/login`,
            credentials
        ).pipe(
            tap(response => {
                sessionStorage.setItem('jwtToken', response.jwtToken);
            })
        );
    }
    register(data: RegisterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(
            `${this.apiUrl}/register`,
            data
        ).pipe(
            tap(response => {
                sessionStorage.setItem('jwtToken', response.jwtToken);
            })
        );
    }

    logout(): void {
        sessionStorage.removeItem('jwtToken');
    }

    getToken(): string | null {
        return sessionStorage.getItem('jwtToken');
    }

    isAuthenticated(): boolean {
        return !!this.getToken();
    }
}