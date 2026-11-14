import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Movies } from './features/movies/movies';
import { Notfound } from './features/notfound/notfound';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'movies', component: Movies },
    { path: '', redirectTo: 'movies', pathMatch: 'full' },
    { path: '**', component: Notfound }
];