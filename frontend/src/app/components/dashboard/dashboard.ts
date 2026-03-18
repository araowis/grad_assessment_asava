import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  stats = [
    { label: 'Total Value', value: '$41,431.50', sub: '3 assets', icon: '💲', color: 'text-blue-500' },
    { label: 'Total Gain', value: '$3,056.50', sub: '7.96% return', icon: '📈', color: 'text-emerald-500' },
    { label: 'Available Cash', value: '$25,000.00', sub: 'Ready to invest', icon: '🏦', color: 'text-blue-400' },
    { label: 'Top Gainer', value: 'SOL', sub: '+6.51% today', icon: '🔝', color: 'text-emerald-400' }
  ];

  marketData = [
    { symbol: 'BTC', name: 'Bitcoin', price: '$64,231.00', change: '+2.4%', color: 'bg-orange-500' },
    { symbol: 'ETH', name: 'Ethereum', price: '$3,452.12', change: '-1.1%', color: 'bg-blue-500' },
    { symbol: 'SOL', name: 'Solana', price: '$145.67', change: '+6.5%', color: 'bg-purple-500' }
  ];
}
