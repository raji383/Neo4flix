
import { Component, input, signal } from '@angular/core';

@Component({
  selector: 'app-share-movie',
  templateUrl: './share-movie.html',
  styleUrl: './share-movie.scss',
})
export class ShareMovie {
  readonly movieId = input.required<string>();
  readonly title = input.required<string>();

  readonly copied = signal(false);
  readonly sharing = signal(false);
  readonly error = signal<string | null>(null);

  private getShareData(): ShareData & { url: string } {
    const url = new URL(
      `/movies/${encodeURIComponent(this.movieId())}`,
      window.location.origin,
    ).toString();

    return {
      title: this.title(),
      text: `Check out ${this.title()} on Neo4flix!`,
      url,
    };
  }

  async share(): Promise<void> {
    this.error.set(null);

    const data = this.getShareData();

    if (typeof navigator.share !== 'function') {
      await this.copyLink();
      return;
    }

    if (navigator.canShare && !navigator.canShare(data)) {
      await this.copyLink();
      return;
    }

    this.sharing.set(true);

    try {
      await navigator.share(data);
    } catch (error: unknown) {
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }

      this.error.set('Could not share this movie. Try copying the link.');
    } finally {
      this.sharing.set(false);
    }
  }

  async copyLink(): Promise<void> {
    this.error.set(null);

    try {
      await navigator.clipboard.writeText(this.getShareData().url);
      this.copied.set(true);

      window.setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.error.set('Could not copy the link. Please try again.');
    }
  }
}
