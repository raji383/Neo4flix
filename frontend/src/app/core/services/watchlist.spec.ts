import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { WatchlistService } from './watchlist.service';

describe('WatchlistService', () => {
  let service: WatchlistService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WatchlistService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the signed-in user watchlist from the rating-service route', () => {
    service.getWatchlist().subscribe((items) => expect(items).toEqual([]));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/watchlist`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('adds a movie using the movie-id route', () => {
    service.create('movie 1').subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/watchlist/movie%201`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 201, statusText: 'Created' });
  });
});
