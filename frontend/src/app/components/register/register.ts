import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterDTO } from '../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, CommonModule, ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register implements OnInit {
  registerForm!: FormGroup;
  isLoading = signal(false);
  successMessage = signal('');
  errorMessage = signal('');
  showPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.initializeForm();
  }

  /**
   * Initialize registration form with validation
   */
  initializeForm() {
    this.registerForm = this.fb.group({
      fullName: ['', [
        Validators.required,
        Validators.minLength(3),
      ]],
      username: ['', [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(20),
      ]],
      email: ['', [
        Validators.required,
        Validators.email,
      ]],
      phoneNumber: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]{10}$/), // 10 digit phone number
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        this.passwordStrengthValidator,
      ]],
    });
  }

  /**
   * Custom validator for password strength
   */
  passwordStrengthValidator = (control: any) => {
    const value = control.value;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumbers = /\d/.test(value);
    const hasSpecialChar = /[!@#$%^&*]/.test(value);

    const passwordValid = hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar;

    return !passwordValid ? { weakPassword: true } : null;
  };

  /**
   * Handle form submission
   */
  onRegister() {
    if (this.registerForm.invalid) {
      this.setError('Please fill in all fields correctly');
      console.warn('❌ Form invalid:', this.registerForm.errors);
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    // Prepare form data
    const registerData: RegisterDTO = this.registerForm.value;

    console.log('📝 Attempting registration with:', {
      ...registerData,
      password: '***',
    });

    // Call auth service
    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('✅ Registration successful!');
        this.setSuccess('Registration successful! Redirecting to dashboard...');
        this.isLoading.set(false);

        // Redirect after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/app/dashboard']);
        }, 2000);
      },
      error: (err) => {
        console.error('❌ Registration failed:',err);
        this.isLoading.set(false);
        const errorMsg = err.message || 'Registration failed. Please try again.';
        this.setError(errorMsg);
      },
    });
  }

  /**
   * Toggle password visibility
   */
  togglePasswordVisibility() {
    this.showPassword.update((v) => !v);
  }

  /**
   * Clear all messages
   */
  private clearMessages() {
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  /**
   * Set success message
   */
  private setSuccess(msg: string) {
    this.successMessage.set(msg);
    this.errorMessage.set('');
  }

  /**
   * Set error message
   */
  private setError(msg: string) {
    this.errorMessage.set(msg);
    this.successMessage.set('');
  }

  /**
   * Helper methods for template
   */
  get fullName() {
    return this.registerForm.get('fullName');
  }

  get username() {
    return this.registerForm.get('username');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get phoneNumber() {
    return this.registerForm.get('phoneNumber');
  }

  get password() {
    return this.registerForm.get('password');
  }
}
