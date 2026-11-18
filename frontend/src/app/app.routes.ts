import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { REGISTER } from './features/auth/register/register';
import { Movies } from './features/movies/movies';
import { MovieDetails } from './features/movie-details/movie-details';
import { Notfound } from './features/notfound/notfound';
import { Profile } from './features/profile/profile';
import { Page } from './layout/page/page';
import { Recommendations } from './features/recommendations/recommendations';
import { Ratings } from './features/ratings/ratings';
import { guestGuard } from './core/guards/guest-guard';
import { authGuard } from './core/guards/auth-guard';
import { Dashboard } from './features/dashboard/dashboard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
    { path: 'login', component: Login, canActivate: [guestGuard] },
    { path: 'register', component: REGISTER, canActivate: [guestGuard] },
    { path: '', redirectTo: 'movies', pathMatch: 'full' },
    {
        path: '',
        component: Page,
        canActivate: [authGuard],
        children: [
            { path: 'movies', component: Movies },
            { path: 'movies/:id', component: MovieDetails },
            { path: 'profile', component: Profile },
            { path: 'recommendations', component: Recommendations },
            { path: 'rating', component: Ratings },
            { path: 'dashboard', component: Dashboard, canActivate: [roleGuard(['ADMIN'])] },
        ]
    },
    { path: '**', component: Notfound }
];