
import { Component, input, signal } from '@angular/core';

@Component({
  selector: 'app-share-movie',
  templateUrl: './share-movie.html',
  styleUrl: './share-movie.scss',
})
export class ShareMovie {
  readonly movieId = input.required<string>();

  readonly copied = signal(false);
  readonly error = signal<string | null>(null);

  private getShareUrl(): string {
    return new URL(
      `/movies/${encodeURIComponent(this.movieId())}`,
      window.location.origin,
    ).toString();
  }

  async copyLink(): Promise<void> {
    this.error.set(null);

    try {
      await navigator.clipboard.writeText(this.getShareUrl());
      this.copied.set(true);

      window.setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.error.set('Could not copy the link. Please try again.');
    }
  }
}
