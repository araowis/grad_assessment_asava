import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout implements OnInit {
  constructor(
    private authService: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    // Initial check is handled by the template getter,
    // but we can trigger a manual check if needed
    this.cdr.detectChanges();
  }

  // Getter to check login status dynamically
  get isLoggedIn(): boolean {
    return !!this.authService.getToken();
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.cdr.detectChanges();
  }
}
