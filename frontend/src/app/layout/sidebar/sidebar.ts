import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { UserProfile } from '../../shared/models/user-profile.model';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-sidebar',
  styleUrl: './sidebar.scss',
  templateUrl: './sidebar.html',
})
export class Sidebar implements OnInit {
  private userProfileService = inject(UserProfileService);
  private errorMessageService = inject(ErrorMessageService);
  private router = inject(Router);
  readonly user = signal<UserProfile | null>(null);
  collapsed = signal(false);
  profileError = signal<string | null>(null);
  private authService = inject(AuthService);
  private errorMessageService = inject(ErrorMessageService);

  toggle() {
    this.collapsed.update(v => !v);
  }
  ngOnInit() {
    this.userProfileService.getProfile().subscribe({
      next: (profile) => {
        this.user.set(profile);
      },
      error: (error: unknown) => {
        this.profileError.set(
          this.errorMessageService.getHttpErrorMessage(error, 'Could not load your profile.'),
        );
      }
    });
  }

  onLogout() {

    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
