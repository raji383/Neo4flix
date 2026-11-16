import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-sidebar',
  styleUrl: './sidebar.scss',
  templateUrl: './sidebar.html',
})
export class Sidebar {
  collapsed = signal(false);

  toggle() {
    this.collapsed.update(v => !v);
  }

  onLogout() {
    // call your auth service logout(), then navigate to /login
  }
}
