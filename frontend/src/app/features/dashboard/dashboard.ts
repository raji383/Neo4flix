import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CreateMovie, Movie, MovieSearchParams } from '../../shared/models/movie';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageResponse } from '../../shared/models/pageResponse';
import { NgTemplateOutlet } from '@angular/common';
import { EMPTY, catchError, switchMap } from 'rxjs';
import { ErrorMessageService } from '../../core/services/error-message.service';
import { MovieService } from '../../core/services/movie.service';
import { NotificationService } from '../../core/services/notification.service';
type FormMode = 'create' | 'edit';

@Component({
  imports: [NgTemplateOutlet,ReactiveFormsModule],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly movieService = inject(MovieService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly notifications = inject(NotificationService);
  readonly maxYear = new Date().getFullYear() + 5;

  readonly movies = signal<Movie[]>([]);
  readonly genreOptions = signal<string[]>([]);
  readonly moviesError = signal<string | null>(null);
  readonly genresError = signal<string | null>(null);

  readonly params = signal<MovieSearchParams>({ q: '', page: 0, size: 5, genres: [] });

  readonly view = signal<PageResponse<Movie>>({
    content: [],
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 1,
  });

  readonly stats = computed(() => {
    const list = this.movies();
    const rated = list.filter((m) => m.averageRating != null);
    const avg = rated.length ? rated.reduce((s, m) => s + (m.averageRating ?? 0), 0) / rated.length : 0;
    return {
      total: this.view().totalElements,
      ratings: list.reduce((s, m) => s + m.ratingCount, 0),
      average: avg,
      genres: this.genreOptions().length,
    };
  });

  constructor() {
    toObservable(this.params)
      .pipe(
        switchMap((params) => {
          this.moviesError.set(null);
          return this.movieService.search(params).pipe(
            catchError((error: unknown) => {
              this.moviesError.set(
                this.errorMessageService.getHttpErrorMessage(error, 'Could not load movies.'),
              );
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        this.view.set(response);
        this.movies.set(response.content);
      });

    this.loadGenres();
  }

  loadGenres(): void {
    this.genresError.set(null);
    this.movieService
      .getGenres()
      .pipe(
        catchError((error: unknown) => {
          this.genresError.set(
            this.errorMessageService.getHttpErrorMessage(error, 'Could not load movie genres.'),
          );
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((genres) => this.genreOptions.set(genres));
  }

  retryMovieSearch(): void {
    this.params.update((params) => ({ ...params }));
  }

  buildForm() {
    return this.fb.nonNullable.group({
      title: ['', [Validators.required, Validators.maxLength(120)]],
      director: ['', [Validators.required, Validators.maxLength(80)]],
      releaseYear: [new Date().getFullYear(), [Validators.required, Validators.min(1888), Validators.max(this.maxYear)]],
      description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(600)]],
      genres: [[] as string[], [Validators.required]],
    });
  }

  readonly createForm = this.buildForm();
  readonly editForm = this.buildForm();

  readonly createFile = signal<File | undefined>(undefined);
  readonly editFile = signal<File | undefined>(undefined);
  readonly createPreview = signal<string | null>(null);
  readonly editPreview = signal<string | null>(null);

  // ---------- dialogs / feedback ----------
  readonly editing = signal<Movie | null>(null);
  readonly deleting = signal<Movie | null>(null);

  // ---------- list actions ----------
  onSearch(q: string) {
    this.params.update((p) => ({ ...p, q, page: 0 }));
  }

  onGenreFilter(genre: string) {
    this.params.update((p) => ({ ...p, genres: genre ? [genre] : [], page: 0 }));
  }

  goTo(page: number) {
    const lastPage = Math.max(0, this.view().totalPages - 1);
    this.params.update((p) => ({ ...p, page: Math.min(Math.max(page, 0), lastPage) }));
  }

  // ---------- create ----------
  submitCreate() {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const payload: CreateMovie = { ...this.createForm.getRawValue(), file: this.createFile() };
    this.movieService
      .create(payload)
      .pipe(
        catchError((error: unknown) => {
          this.notifications.showHttpError(error, 'Could not add the movie.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((movie) => {
        this.params.update((p) => ({ ...p, page: 0 }));
        this.resetCreate();
        this.notifications.showSuccess(`"${movie.title}" was added.`);
      });
  }

  resetCreate() {
    this.createForm.reset({ title: '', director: '', releaseYear: new Date().getFullYear(), description: '', genres: [] });
    this.createFile.set(undefined);
    this.createPreview.set(null);
  }

  // ---------- edit (popup) ----------
  openEdit(movie: Movie) {
    this.editForm.reset({
      title: movie.title,
      director: movie.director,
      releaseYear: movie.releaseYear,
      description: movie.description,
      genres: [...movie.genres],
    });
    this.editFile.set(undefined);
    this.editPreview.set(movie.imageUrl ?? null);
    this.editing.set(movie);
  }

  submitEdit() {
    const current = this.editing();
    if (!current) return;
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const payload: CreateMovie = { ...this.editForm.getRawValue(), file: this.editFile() };
    this.movieService
      .update(current.id, payload)
      .pipe(
        catchError((error: unknown) => {
          this.notifications.showHttpError(error, 'Could not update the movie.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((movie) => {
        this.closeEdit();
        this.params.update((params) => ({ ...params }));
        this.notifications.showSuccess(`"${movie.title}" was updated.`);
      });
  }

  closeEdit() {
    this.editing.set(null);
  }

  // ---------- delete (confirm popup) ----------
  askDelete(movie: Movie) {
    this.deleting.set(movie);
  }

  confirmDelete() {
    const target = this.deleting();
    if (!target) return;
    this.movieService
      .delete(target.id)
      .pipe(
        catchError((error: unknown) => {
          this.notifications.showHttpError(error, 'Could not delete the movie.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        const { page = 0, size = 5 } = this.params();
        const totalAfterDelete = this.view().totalElements - 1;
        const lastPage = Math.max(0, Math.ceil(totalAfterDelete / size) - 1);
        this.params.update((params) => ({ ...params, page: Math.min(page, lastPage) }));
        this.deleting.set(null);
        this.notifications.showSuccess(`"${target.title}" was deleted.`);
      });
  }

  @HostListener('document:keydown.escape')
  closeDialogs() {
    this.editing.set(null);
    this.deleting.set(null);
  }

  // ---------- shared form helpers ----------
  onFile(event: Event, mode: FormMode) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => (mode === 'create' ? this.createPreview : this.editPreview).set(reader.result as string);
    reader.readAsDataURL(file);
    (mode === 'create' ? this.createFile : this.editFile).set(file);
  }

  toggleGenre(form: MovieForm, genre: string) {
    const ctrl = form.controls.genres;
    const list = ctrl.value;
    ctrl.setValue(list.includes(genre) ? list.filter((g) => g !== genre) : [...list, genre]);
    ctrl.markAsTouched();
  }



  invalid(form: MovieForm, name: keyof MovieForm["controls"]) {
    const c = form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  stars(rating?: number | null) {
    const r = rating ?? 0;
    return Array.from({ length: 5 }, (_, i) => (r >= i + 1 ? 'full' : r >= i + 0.5 ? 'half' : 'empty'));
  }

}
type MovieForm = Dashboard["createForm"];
