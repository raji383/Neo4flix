import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Profile } from './profile';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  const profileService = {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  };
  const authService = {
    logout: vi.fn(),
  };
  const watchlistService = {
    getWatchlist: vi.fn(),
    delete: vi.fn(),
  };
  const errorMessageService = {
    getHttpErrorMessage: vi.fn(() => 'Request failed'),
  };

  beforeEach(async () => {
    profileService.getProfile.mockReset().mockReturnValue(of({
      id: 'user-id',
      username: 'filmfan',
      email: 'filmfan@example.com',
      role: 'USER',
      twoFactorEnabled: false,
    }));
    profileService.updateProfile.mockReset();
    authService.logout.mockReset();
    watchlistService.getWatchlist.mockReset().mockReturnValue(of([]));
    watchlistService.delete.mockReset();

    await TestBed.configureTestingModule({
      imports: [Profile],
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
