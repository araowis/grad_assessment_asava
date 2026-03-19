// portfolio-state.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PortfolioStateService {
  private selectedId = new BehaviorSubject<number>(1); // Default to Portfolio 1
  selectedId$ = this.selectedId.asObservable();

  setPortfolio(id: number) {
    this.selectedId.next(id);
  }

  get currentId() {
    return this.selectedId.value;
  }
}
