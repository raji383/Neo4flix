import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from '../navbar/navbar';
import { Sidebar } from '../sidebar/sidebar';

@Component({
  imports: [RouterOutlet, Navbar, Sidebar],
  selector: 'app-page',
  styleUrl: './page.scss',
  templateUrl: './page.html',
})
export class Page {}
