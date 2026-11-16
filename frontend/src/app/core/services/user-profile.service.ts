import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserProfile } from '../../shared/models/user-profile.model';

@Injectable({
    providedIn: 'root'
})
export class UserProfileService {

    private http = inject(HttpClient);

    private readonly apiUrl = `${environment.apiUrl}/api/users/me`;

    getProfile(): Observable<UserProfile> {
        return this.http.get<UserProfile>(this.apiUrl);
    }

    updateProfile(
        data: Pick<UserProfile, 'username' | 'email'>
    ): Observable<UserProfile> {
        return this.http.put<UserProfile>(this.apiUrl, data);
    }
}