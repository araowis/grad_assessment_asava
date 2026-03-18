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

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'companies', component: CompanyComponent },
  {
    path: '',
    component: Layout,
    // canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'trading', component: Trading },
      { path: 'wallet', component: Wallet },
      { path: 'settings', component: Settings },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
