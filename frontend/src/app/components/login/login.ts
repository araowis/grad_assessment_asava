import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../services/auth';
import { ApiResponse, AuthResponse } from '../../models/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  credentials = { emailOrUsername: '', password: '' };

  constructor(private authService: Auth, private router: Router) {}

  onLogin() {
    this.authService.login(this.credentials).subscribe({
      next: (res: ApiResponse<AuthResponse>) => {
        if (res.success && res.data) {
          // AuthService already saves token + currentUser in its tap() handler
          const role = res.data.user?.role;
          console.log("Role found: " + role);

          if (role === 'ROLE_ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            // ROLE_TRADER, ROLE_ANALYST
            this.router.navigate(['/app/dashboard']);
          }
        }
      },
      error: (err: any) => {
        const message = err?.error?.message || err?.message || 'Login failed.';
        alert('Login Failed: ' + message);
      },
    });
  }
}
