import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Movies } from './movies';

describe('Movies', () => {
  let component: Movies;
  let fixture: ComponentFixture<Movies>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Movies],
    }).compileComponents();

    fixture = TestBed.createComponent(Movies);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('applies genre and year selections only when requested', () => {
    const initialParams = component.filterParams();

    component.toggleGenre('Drama');
    component.onReleaseYear('2020');

    expect(component.filterParams()).toEqual(initialParams);

    component.applyFilters();

    expect(component.filterParams()).toEqual({
      ...initialParams,
      genres: ['Drama'],
      releaseYear: 2020,
      page: 0,
    });
  });

  it('allows clearing all genre and year filters', () => {
    component.toggleGenre('Drama');
    component.onReleaseYear('2020');
    component.applyFilters();

    component.toggleGenre('Drama');
    component.onReleaseYear('');
    component.applyFilters();

    expect(component.filterParams().genres).toEqual([]);
    expect(component.filterParams().releaseYear).toBeUndefined();
  });

  it('keeps future years as exact filters instead of clearing them', () => {
    component.onReleaseYear('2500');
    component.applyFilters();

    expect(component.filterParams().releaseYear).toBe(2500);
    expect(component.requestParams().releaseYear).toBe(2500);
  });

  it('keeps search state independent while combining search and applied filters in requests', () => {
    const initialFilters = component.filterParams();

    component.onSearch('Dune');
    component.toggleGenre('Action');
    component.toggleGenre('Comedy');
    component.onReleaseYear('2020');

    expect(component.searchQuery()).toBe('Dune');
    expect(component.filterParams()).toEqual(initialFilters);
    expect(component.requestParams()).toEqual({ ...initialFilters, q: 'Dune' });

    component.applyFilters();

    expect(component.requestParams()).toEqual({
      ...initialFilters,
      q: 'Dune',
      genres: ['Action', 'Comedy'],
      releaseYear: 2020,
    });
  });
});
