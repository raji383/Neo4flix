import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { REGISTER } from './features/auth/register/register';
import { Movies } from './features/movies/movies';
import { Notfound } from './features/notfound/notfound';
import { authGuard } from './core/guards/auth.guard';
import { Profile } from './features/profile/profile';
import { Page } from './layout/page/page';
import { Recommendations } from './features/recommendations/recommendations';
import { Ratings } from './features/ratings/ratings';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'register', component: REGISTER },
    { path: '', redirectTo: 'movies', pathMatch: 'full' },
    {
        path: '',
        component: Page,
        canActivate: [authGuard],
        children: [
            { path: 'movies', component: Movies },
            { path: 'profile', component: Profile },
            { path: 'recommendations', component: Recommendations },
            { path: 'rating', component: Ratings },
        ]
    },
    { path: '**', component: Notfound }
];