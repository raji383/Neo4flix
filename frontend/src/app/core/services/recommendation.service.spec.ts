import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { RecommendationService } from './recommendation.service';

describe('RecommendationService', () => {
  let service: RecommendationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RecommendationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads recommendations for the signed-in user', () => {
    service.getRecommendations().subscribe((recommendations) => expect(recommendations).toEqual([]));

    const request = http.expectOne(`${environment.apiUrl}/api/recommendations`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
