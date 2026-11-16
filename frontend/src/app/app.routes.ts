import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { REGISTER } from './features/auth/register/register';
import { Movies } from './features/movies/movies';
import { Notfound } from './features/notfound/notfound';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'register', component: REGISTER },
    { path: 'movies', component: Movies },
    { path: '', redirectTo: 'movies', pathMatch: 'full' },
    { path: '**', component: Notfound }
];