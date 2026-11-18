import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { UserProfile } from '../../shared/models/user-profile.model';
import { UserProfileService } from '../../core/services/user-profile.service';

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
    localStorage.removeItem('jwtToken');
    this.router.navigate(['/login']);
  }
}
