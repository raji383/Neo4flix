
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
    TwoFactorSetupResponse,
    VerifyEmailRequest,
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
        ).pipe(
            tap(response => {
                if (response.jwtToken) {
                    this.tokenStorage?.setItem('jwtToken', response.jwtToken);
                }
            })
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

    verifyEmail(request: VerifyEmailRequest): Observable<void> {
        return this.http.post<void>(
            `${this.apiUrl}/2fa/email/verify`,
            request
        );
    }

    enableTwoFactor(): Observable<TwoFactorSetupResponse> {
        return this.http.post<TwoFactorSetupResponse>(
            `${this.apiUrl}/2fa/enable`,
            {}
        );
    }

    disableTwoFactor(): Observable<void> {
        return this.http.post<void>(
            `${this.apiUrl}/2fa/disable`,
            {}
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
