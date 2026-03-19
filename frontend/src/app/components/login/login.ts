import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../services/auth';
import { ApiResponse, AuthResponse } from '../../models/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  credentials = { emailOrUsername: '', password: '' };
  loading = false;
  errorMsg = '';

  constructor(
    private authService: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  onLogin() {
    this.errorMsg = '';

    if (!this.credentials.emailOrUsername || !this.credentials.password) {
      this.errorMsg = 'Please enter both username and password.';
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    this.authService.login(this.credentials).subscribe({
      next: (res: ApiResponse<AuthResponse>) => {
        if (res.success && res.data) {
          const role = res.data.user?.role;
          if (role === 'ROLE_ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/app/dashboard']);
          }
        } else {
          this.errorMsg = 'Invalid login response. Please try again.';
          this.loading = false;
        }
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        // 🟢 Kill loading immediately so the button and errorMsg become visible
        this.loading = false;

        if (err.status === 401) {
          this.errorMsg = 'Invalid username or password.';
        } else if (err.status === 403) {
          this.errorMsg = 'Account is locked or restricted.';
        } else {
          this.errorMsg = err?.error?.message || 'Connection error. Please try again.';
        }

        console.error('Login Error:', err);

        // 🟢 Force Angular to recognize the errorMsg change
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
    });
  }
}
