import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Movie } from '../../shared/models/movie';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne((req) => req.url === `${environment.apiUrl}/api/movies`).flush({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    });
    http.expectOne((req) => req.url === `${environment.apiUrl}/api/genres`).flush(['Action']);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads movies and genres from the API', async () => {
    component.onSearch('Dune');
    fixture.detectChanges();
    await fixture.whenStable();

    const request = http.expectOne(
      (req) => req.url === `${environment.apiUrl}/api/movies` && req.params.get('q') === 'Dune',
    );
    expect(request.request.params.get('page')).toBe('0');
    request.flush({
      content: [
        {
          id: 'movie-1',
          title: 'Dune',
          director: 'Denis Villeneuve',
          description: 'A science fiction film.',
          releaseYear: 2021,
          genres: ['Science Fiction'],
          imageUrl: null,
          averageRating: 4.5,
          ratingCount: 12,
        } satisfies Movie,
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();

    expect(component.movies()[0].title).toBe('Dune');
    expect(component.view().totalElements).toBe(1);
    expect(component.genreOptions()).toEqual(['Action']);
  });

  it('creates a movie through the service and reloads the catalog', async () => {
    component.createForm.setValue({
      title: 'Dune',
      director: 'Denis Villeneuve',
      releaseYear: 2021,
      description: 'A science fiction film on Arrakis.',
      genres: ['Action'],
    });

    component.submitCreate();
    const createRequest = http.expectOne(`${environment.apiUrl}/api/movies/add`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toBeInstanceOf(FormData);
    createRequest.flush({
      id: 'movie-1',
      title: 'Dune',
      director: 'Denis Villeneuve',
      description: 'A science fiction film on Arrakis.',
      releaseYear: 2021,
      genres: ['Action'],
      imageUrl: null,
      averageRating: null,
      ratingCount: 0,
    } satisfies Movie);
    fixture.detectChanges();
    await fixture.whenStable();

    const refreshRequest = http.expectOne(
      (req) => req.url === `${environment.apiUrl}/api/movies` && req.params.get('page') === '0',
    );
    refreshRequest.flush({
      content: [
        {
          id: 'movie-1',
          title: 'Dune',
          director: 'Denis Villeneuve',
          description: 'A science fiction film on Arrakis.',
          releaseYear: 2021,
          genres: ['Action'],
          imageUrl: null,
          averageRating: null,
          ratingCount: 0,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();

    expect(component.movies().map((movie) => movie.id)).toEqual(['movie-1']);
    expect(component.createForm.controls.title.value).toBe('');
  });
});
