import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../services/auth';

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
      next: (res) => {
        localStorage.setItem('token', res.data.accessToken);
        this.router.navigate(['/']);
      },
      error: (err) => alert('Login Failed: ' + err.error.message)
    });
  }
}
