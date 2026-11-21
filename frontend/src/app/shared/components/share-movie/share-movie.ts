
import { Component, inject, input, signal } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-share-movie',
  templateUrl: './share-movie.html',
  styleUrl: './share-movie.scss',
})
export class ShareMovie {
  readonly movieId = input.required<string>();

  readonly copied = signal(false);
  private readonly notifications = inject(NotificationService);

  private getShareUrl(): string {
    return new URL(
      `/movies/${encodeURIComponent(this.movieId())}`,
      window.location.origin,
    ).toString();
  }

  async copyLink(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.getShareUrl());
      this.copied.set(true);
      this.notifications.showSuccess('Movie link copied to clipboard.');

      window.setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.notifications.showError('Could not copy the link. Please try again.');
    }
  }
}
