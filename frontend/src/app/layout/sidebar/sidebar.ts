import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-sidebar',
  styleUrl: './sidebar.scss',
  templateUrl: './sidebar.html',
})
export class Sidebar {
  collapsed = signal(false);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);
  private router = inject(Router);

  toggle() {
    this.collapsed.update(v => !v);
  }

  onLogout() {

    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
