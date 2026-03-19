import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
})
export class Settings {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  showCurrent = false;
  showNew = false;
  showConfirm = false;

  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private http: HttpClient,
    private authService: Auth,
    private cdr: ChangeDetectorRef
  ) {}

  get user() {
    return this.authService.getCurrentUser();
  }

  get passwordStrength(): { label: string; score: number; color: string } {
    const p = this.newPassword;
    if (!p) return { label: '', score: 0, color: '' };
    let score = 0;
    if (p.length >= 8) score++;
    if (/[A-Z]/.test(p)) score++;
    if (/[0-9]/.test(p)) score++;
    if (/[^A-Za-z0-9]/.test(p)) score++;
    if (p.length >= 12) score++;
    const map = [
      { label: 'Very Weak', color: '#ef4444' },
      { label: 'Weak', color: '#f97316' },
      { label: 'Fair', color: '#eab308' },
      { label: 'Strong', color: '#22c55e' },
      { label: 'Very Strong', color: '#10b981' },
    ];
    return { ...map[Math.min(score - 1, 4)], score };
  }

  get isValid(): boolean {
    return (
      !!this.currentPassword &&
      !!this.newPassword &&
      this.newPassword === this.confirmPassword &&
      this.newPassword !== this.currentPassword &&
      this.newPassword.length >= 8
    );
  }

  changePassword() {
    if (!this.isValid) return;
    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.http.patch('http://localhost:8088/api/v1/auth/change-password', {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword,
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Password updated successfully.';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to update password. Check your current password.';
        this.cdr.detectChanges();
      }
    });
  }
}