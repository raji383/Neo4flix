import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { REGISTER } from './features/auth/register/register';
import { Movies } from './features/movies/movies';
import { Notfound } from './features/notfound/notfound';
import { authGuard } from './core/guards/auth.guard';
import { Profile } from './features/profile/profile';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'register', component: REGISTER },
    { path: 'movies', component: Movies, canActivate: [authGuard] },
    { path: 'profile', component: Profile, canActivate: [authGuard] },
    { path: '', redirectTo: 'movies', pathMatch: 'full' },
    { path: '**', component: Notfound }
];