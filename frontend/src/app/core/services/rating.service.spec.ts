import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { RatingService } from './rating.service';

describe('RatingService', () => {
  let service: RatingService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/ratings`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RatingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the signed-in user ratings', () => {
    service.getMyRatings().subscribe((ratings) => expect(ratings).toEqual([]));

    const request = http.expectOne(`${baseUrl}/me`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('loads the current user rating for a movie', () => {
    service.getMyRatingToMovie('movie-1').subscribe((rating) => expect(rating).toBe(4));

    const request = http.expectOne(`${baseUrl}/movie-1`);
    expect(request.request.method).toBe('GET');
    request.flush(4);
  });

  it('posts a new movie rating', () => {
    service.addRating('movie-1', 4).subscribe();

    const request = http.expectOne(`${baseUrl}/movie-1`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ value: 4 });
    request.flush(null);
  });

  it('updates an existing movie rating', () => {
    service.updateRating('movie-1', 5).subscribe();

    const request = http.expectOne(`${baseUrl}/movie-1`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ value: 5 });
    request.flush(null);
  });

  it('deletes a movie rating', () => {
    service.deleteRating('movie-1').subscribe();

    const request = http.expectOne(`${baseUrl}/movie-1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
