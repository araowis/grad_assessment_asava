import { Routes } from '@angular/router';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { CompanyComponent } from './components/company/company';
import { authGuard } from './guards/auth-guard';
import { Trading } from './components/trading/trading';
import { Dashboard } from './components/dashboard/dashboard';
import { Layout } from './components/layout/layout';
import { Wallet } from './components/wallet/wallet';
import { Settings } from './components/settings/settings';
import { Landing } from './components/landing/landing';
import { guestGuard } from './guards/guest-guard';
import { CompanyDetailsComponent } from './components/company-details/company-details.component';
import { AdminAddCompany } from './components/admin/admin-add-company/admin-add-company';
import { AdminCompanies } from './components/admin/admin-companies/admin-companies';
import { AdminDashboard } from './components/admin/admin-dashboard/admin-dashboard';
import { AdminLayout } from './components/admin/admin-layout/admin';

export const routes: Routes = [
  { path: '', component: Landing, canActivate: [guestGuard] },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: Register, canActivate: [guestGuard] },
  { path: 'companies', component: CompanyComponent },
  {
    path: 'app',
    component: Layout,
    // canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'company-details/:symbol', component: CompanyDetailsComponent },
      { path: 'trading', component: Trading },
      { path: 'wallet', component: Wallet },
      { path: 'settings',component: Settings },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  // Legacy redirect — keeps old /dashboard links working
  { path: 'dashboard', redirectTo: '/app/dashboard', pathMatch: 'full' },

  // Admin
  {
    path: 'admin',
    component: AdminLayout,
    // canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboard },
      { path: 'companies', component: AdminCompanies },
      { path: 'companies/add', component: AdminAddCompany },
    ],
  },

  { path: '**', redirectTo: '' },
];
