import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ApiResponse, AuthResponse, LoginDTO } from '../../models/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  credentials: LoginDTO = { emailOrUsername: '', password: '' };

  constructor(private authService: AuthService, private router: Router) {}

  onLogin() {
    this.authService.login(this.credentials).subscribe({
      next: (res: ApiResponse<AuthResponse>) => {
        if (res.success && res.data) {
          localStorage.setItem('token', res.data.accessToken);
          this.router.navigate(['/']);
        }
      },
      error: (err: any) => {
        const message = err?.error?.message || err?.message || 'Login failed.';
        alert('Login Failed: ' + message);
      }
    });
  }
}
