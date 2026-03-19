import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  user = { username: '', email: '', password: '', fullName: '', phoneNumber: '' };

  constructor(private authService: Auth, private router: Router) {}

  onRegister() {
    this.authService.register(this.user).subscribe({
      next: () => {
        alert('Registration Successful!');
        this.router.navigate(['/login']);
      },
      error: (err) => alert('Registration Failed: ' + err.error.message)
    });
  }
}
