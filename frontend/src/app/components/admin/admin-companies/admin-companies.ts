import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminService, Company } from '../../../services/admin';

@Component({
  selector: 'app-admin-companies',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin-companies.html',
})
export class AdminCompanies implements OnInit {
  companies: Company[] = [];
  filtered: Company[] = [];
  searchTerm = '';
  loading = true;

  // Inline price editing
  editingId: number | null = null;
  editPrice = 0;

  // Toast notification
  toast = '';
  toastType: 'success' | 'error' = 'success';

  constructor(private adminService: AdminService) { }

  ngOnInit() {
    this.adminService.getCompanies().subscribe({
      next: (data) => {
        this.companies = data.map((c: any, index: number) => ({
          id: c.shortId,
          name: c.name,
          stockSymbol: c.shortId,
          totalStocks: c.noOfShare,
          currentPrice: c.currentPrice
        }));
        this.filtered = this.companies;
        this.loading = false;
      },
      error: () => {
        // ── Dev fallback ──
        this.companies = [
          { id: 1, name: 'Infosys Ltd', stockSymbol: 'INFY', totalStocks: 420000000, currentPrice: 1482.50 },
          { id: 2, name: 'Tata Consultancy', stockSymbol: 'TCS', totalStocks: 365000000, currentPrice: 3748.00 },
          { id: 3, name: 'Reliance Ind.', stockSymbol: 'RIL', totalStocks: 676000000, currentPrice: 2820.30 },
          { id: 4, name: 'HDFC Bank', stockSymbol: 'HDFC', totalStocks: 556000000, currentPrice: 1647.80 },
          { id: 5, name: 'Wipro Ltd', stockSymbol: 'WIPR', totalStocks: 548000000, currentPrice: 452.10 },
        ];
        this.filtered = this.companies;
        this.loading = false;
      },
    });
  }

  onSearch() {
    const t = this.searchTerm.toLowerCase();
    this.filtered = this.companies.filter(
      (c) => c.name.toLowerCase().includes(t) || c.stockSymbol.toLowerCase().includes(t)
    );
  }

  startEdit(c: Company) {
    this.editingId = c.id!;
    this.editPrice = c.currentPrice;
  }

  cancelEdit() {
    this.editingId = null;
  }

  savePrice(c: Company) {
    const newPrice = this.editPrice;
    this.adminService.updatePrice(c.id!, newPrice).subscribe({
      next: (updated) => {
        c.currentPrice = updated.currentPrice;
      },
      error: () => {
        // Dev fallback: apply locally
        c.currentPrice = newPrice;
      },
    });
    this.editingId = null;
    this.showToast(`Price updated for ${c.name}`, 'success');
  }

  delete(c: Company) {
    if (!confirm(`Remove "${c.name}" from the platform? This cannot be undone.`)) return;

    this.adminService.deleteCompany(c.id!).subscribe({
      next: () => { },
      error: () => { }, // dev: still remove locally
    });
    this.companies = this.companies.filter((x) => x.id !== c.id);
    this.filtered = this.filtered.filter((x) => x.id !== c.id);
    this.showToast(`${c.name} removed from platform`, 'success');
  }

  private showToast(msg: string, type: 'success' | 'error') {
    this.toast = msg;
    this.toastType = type;
    setTimeout(() => (this.toast = ''), 3000);
  }
}