import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { WatchlistService } from '../../core/services/watchlist.service';
import { Watchlist } from './watchlist';

describe('Watchlist', () => {
  let component: Watchlist;
  let fixture: ComponentFixture<Watchlist>;
  const savedMovies = [
    { id: 'movie-1', title: 'Saved movie', imageUrl: '', releaseYear: 2020, addedAt: '' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Watchlist],
      providers: [
        provideRouter([]),
        {
          provide: WatchlistService,
          useValue: {
            getWatchlist: () => of(savedMovies),
            delete: () => of<void>(undefined),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Watchlist);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('removes a movie from the list after the remove action succeeds', () => {
    component.remove('movie-1');

    expect(component.movies()).toEqual([]);
  });
});
