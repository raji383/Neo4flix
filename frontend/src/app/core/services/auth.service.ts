
import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
    AuthResponse,
    LoginChallengeResponse,
    LoginRequest,
    RegisterRequest,
    RegistrationResponse,
    VerifyTwoFactorRequest
} from '../../shared/models/auth';

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

    login(credentials: LoginRequest): Observable<LoginChallengeResponse> {
        return this.http.post<LoginChallengeResponse>(
            `${this.apiUrl}/login`,
            credentials
        );
    }

    verifyTwoFactor(
        request: VerifyTwoFactorRequest
    ): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(
            `${this.apiUrl}/2fa/verify`,
            request
        ).pipe(
            tap(response => {
                this.tokenStorage?.setItem('jwtToken', response.jwtToken);
            })
        );
    }

    register(data: RegisterRequest): Observable<RegistrationResponse> {
        return this.http.post<RegistrationResponse>(
            `${this.apiUrl}/register`,
            data
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
