import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OtpVerificationComponent } from './otp-verification';

describe('OtpVerificationComponent', () => {
  let component: OtpVerificationComponent;
  let fixture: ComponentFixture<OtpVerificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OtpVerificationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OtpVerificationComponent);
    fixture.componentRef.setInput('heading', 'Verify your email');
    fixture.componentRef.setInput('subtitle', 'Enter your code.');
    fixture.componentRef.setInput('submitText', 'Verify');
    fixture.componentRef.setInput('loadingText', 'Verifying...');
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not emit an invalid code', () => {
    const submitted = vi.fn();
    component.codeSubmitted.subscribe(submitted);
    component.otpForm.controls.otp.setValue('12345');

    component.submitCode();

    expect(submitted).not.toHaveBeenCalled();
    expect(component.otpForm.controls.otp.touched).toBe(true);
  });

  it('emits a valid six-digit code', () => {
    const submitted = vi.fn();
    component.codeSubmitted.subscribe(submitted);
    component.otpForm.controls.otp.setValue('123456');

    component.submitCode();

    expect(submitted).toHaveBeenCalledWith('123456');
  });
});