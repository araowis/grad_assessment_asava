import { Component ,OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Company } from '../../models/company';
import { CompanyService } from '../../services/company';

@Component({
  selector: 'app-company',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './company.html',
  styleUrl: './company.css',
})
export class CompanyComponent implements OnInit {
  companies: Company[] = [];
  companyForm: Company = { shortId: 'Test', name: '', noOfShare: 0, currentPrice: 0, openingPrice: 0 };
  isEditMode = false;

  constructor(private companyService: CompanyService) {}

  ngOnInit(): void {
    this.loadCompanies();
  }

  loadCompanies() {
    this.companyService.getAllCompanies().subscribe(data => this.companies = data);
  }

  onSubmit() {
    if (this.isEditMode) {
      this.companyService.updateCompany(this.companyForm.shortId, this.companyForm).subscribe(() => {
        this.resetForm();
        this.loadCompanies();
      });
    } else {
      this.companyService.addCompany(this.companyForm).subscribe(() => {
        this.resetForm();
        this.loadCompanies();
      });
    }
  }

  editCompany(company: Company) {
    this.companyForm = { ...company };
    this.isEditMode = true;
  }

  deleteCompany(id: string) {
    if(confirm('Are you sure?')) {
      this.companyService.deleteCompany(id).subscribe(() => this.loadCompanies());
    }
  }

  resetForm() {
    this.companyForm = { shortId: '', name: '', noOfShare: 0, currentPrice: 0, openingPrice: 0 };
    this.isEditMode = false;
  }
}
