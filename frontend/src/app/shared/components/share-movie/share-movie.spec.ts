import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShareMovie } from './share-movie';

describe('ShareMovie', () => {
  let component: ShareMovie;
  let fixture: ComponentFixture<ShareMovie>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShareMovie],
    }).compileComponents();

    fixture = TestBed.createComponent(ShareMovie);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
