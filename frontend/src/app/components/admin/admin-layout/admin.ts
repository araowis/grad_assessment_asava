import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Auth } from '../../../services/auth';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
})
export class AdminLayout {
  user = JSON.parse(localStorage.getItem('currentUser') || '{}');

  constructor(private router: Router, private authService: Auth) {}

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}