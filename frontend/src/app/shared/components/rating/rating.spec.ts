import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { RatingService } from '../../../core/services/rating.service';
import { RatingComponent } from './rating';

describe('RatingComponent', () => {
  let component: RatingComponent;
  let fixture: ComponentFixture<RatingComponent>;
  const ratingService = {
    getMyRatingToMovie: vi.fn().mockReturnValue(of(3)),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingComponent],
      providers: [
        { provide: RatingService, useValue: ratingService },
        { provide: NotificationService, useValue: { showSuccess: vi.fn(), showHttpError: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RatingComponent);
    fixture.componentRef.setInput('movieId', 'movie-123');
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads the current user rating for the selected movie', () => {
    expect(component).toBeTruthy();
    expect(ratingService.getMyRatingToMovie).toHaveBeenCalledWith('movie-123');
    expect(component.myRating()).toBe(3);
  });
});
