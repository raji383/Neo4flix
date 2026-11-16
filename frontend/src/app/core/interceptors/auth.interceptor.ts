import { inject } from '@angular/core';
import {
    HttpInterceptorFn
} from '@angular/common/http';

import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

    const authService = inject(AuthService);
    const token = authService.getToken();

    const isApiRequest = req.url.startsWith(environment.apiUrl);

    const isPublicAuthRequest =
        req.url.includes('/api/auth/login') ||
        req.url.includes('/api/auth/register');

    if (!token || !isApiRequest || isPublicAuthRequest) {
        return next(req);
    }

    const authReq = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authReq);
};