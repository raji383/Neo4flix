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
export class Sidebar implements OnInit {
  private router = inject(Router);
  private userProfileService = inject(UserProfileService);
  private user = signal<UserProfile | null>(null);
  collapsed = signal(false);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);
  private router = inject(Router);

  toggle() {
    this.collapsed.update(v => !v);
  }
  ngOnInit() {
    this.userProfileService.getProfile().subscribe({
      next: (profile) => {
        this.user.set(profile);
      },
      error: (error) => {
        console.error('Error fetching user profile:', error);
      }
    });
  }

  onLogout() {

    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
