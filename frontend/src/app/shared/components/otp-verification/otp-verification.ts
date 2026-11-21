import { Component, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './otp-verification.html',
  styleUrl: './otp-verification.scss',
})
export class OtpVerificationComponent {
  private readonly formBuilder = inject(FormBuilder);

  heading = input.required<string>();
  subtitle = input.required<string>();
  submitText = input.required<string>();
  loadingText = input.required<string>();
  errorMessage = input('');
  isLoading = input(false);
  codeSubmitted = output<string>();

  otpForm = this.formBuilder.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submitCode(): void {
    if (this.otpForm.invalid || this.isLoading()) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.codeSubmitted.emit(this.otpForm.controls.otp.value);
  }
}