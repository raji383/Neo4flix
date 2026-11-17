import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MovieService } from './movie.service';

describe('MovieService', () => {
  let service: MovieService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(MovieService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends search filters and repeated genre parameters to the API', () => {
    service
      .search({ q: 'neon', page: 0, size: 12, genres: ['Action', 'Sci-Fi'], releaseYear: 2022 })
      .subscribe((response) => expect(response.content).toEqual([]));

    const request = http.expectOne((req) => req.url === `${environment.apiUrl}/api/movies`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('q')).toBe('neon');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.getAll('genre')).toEqual(['Action', 'Sci-Fi']);
    expect(request.request.params.get('releaseYear')).toBe('2022');
    request.flush({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 });
  });

  it('requests popular movies as a list', () => {
    service.getPopular(8).subscribe((movies) => expect(movies).toEqual([]));

    const request = http.expectOne(
      (req) => req.url === `${environment.apiUrl}/api/movies/popular` && req.params.get('limit') === '8',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
