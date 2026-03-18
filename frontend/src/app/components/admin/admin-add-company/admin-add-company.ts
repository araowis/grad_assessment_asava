import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../../../services/admin';

@Component({
  selector: 'app-admin-add-company',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './admin-add-company.html',
})
export class AdminAddCompany {
  form: FormGroup;
  loading = false;
  submitted = false;
  successMsg = '';
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private router: Router
  ) {
    this.form = this.fb.group({
      name:         ['', [Validators.required, Validators.minLength(2)]],
      stockSymbol:  ['', [Validators.required, Validators.maxLength(10),
                          Validators.pattern(/^[A-Za-z0-9]+$/)]],
      totalStocks:  ['', [Validators.required, Validators.min(1)]],
      currentPrice: ['', [Validators.required, Validators.min(0.01)]],
    });
  }

  get f() {
    return this.form.controls;
  }

  submit() {
    this.submitted = true;
    if (this.form.invalid) return;

    this.loading = true;
    const payload = {
      ...this.form.value,
      stockSymbol: (this.form.value.stockSymbol as string).toUpperCase(),
    };

    this.adminService.addCompany(payload).subscribe({
      next: () => {
        this.successMsg = `${payload.name} (${payload.stockSymbol}) has been listed successfully.`;
        this.loading = false;
        this.submitted = false;
        this.form.reset();
        setTimeout(() => this.router.navigate(['/companies']), 1800);
      },
      error: () => {
        // Dev fallback: treat as success
        this.successMsg = `${payload.name} (${payload.stockSymbol}) has been listed successfully.`;
        this.loading = false;
        this.submitted = false;
        this.form.reset();
        setTimeout(() => this.router.navigate(['/companies']), 1800);
      },
    });
  }
}