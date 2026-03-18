import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let compiled: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
    
    // Clear localStorage before each test
    localStorage.clear();
    
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Market View Toggle', () => {
    it('should initialize with card view as default', () => {
      expect(component.marketViewMode).toBe('card');
      expect(component.isCardView()).toBe(true);
      expect(component.isListView()).toBe(false);
    });

    it('should toggle from card to list view', () => {
      component.toggleMarketView();
      expect(component.marketViewMode).toBe('list');
      expect(component.isListView()).toBe(true);
      expect(component.isCardView()).toBe(false);
    });

    it('should toggle from list back to card view', () => {
      component.marketViewMode = 'list';
      component.toggleMarketView();
      expect(component.marketViewMode).toBe('card');
      expect(component.isCardView()).toBe(true);
    });

    it('should save toggle state to localStorage', () => {
      component.toggleMarketView();
      expect(localStorage.getItem('marketViewMode')).toBe('list');
      
      component.toggleMarketView();
      expect(localStorage.getItem('marketViewMode')).toBe('card');
    });

    it('should load saved toggle state from localStorage on init', () => {
      localStorage.setItem('marketViewMode', 'list');
      
      // Create a new component instance to test constructor
      const newComponent = new Dashboard();
      expect(newComponent.marketViewMode).toBe('list');
    });

    it('should render card view by default', () => {
      fixture.detectChanges();
      const cardView = compiled.query(By.css('[data-testid="market-cards"]'));
      const listView = compiled.query(By.css('[data-testid="market-list"]'));
      
      expect(cardView).toBeTruthy();
      expect(listView).toBeFalsy();
    });

    it('should render list view after toggle', () => {
      component.toggleMarketView();
      fixture.detectChanges();
      
      const cardView = compiled.query(By.css('[data-testid="market-cards"]'));
      const listView = compiled.query(By.css('[data-testid="market-list"]'));
      
      expect(cardView).toBeFalsy();
      expect(listView).toBeTruthy();
    });

    it('should have toggle button with correct label in card mode', () => {
      component.marketViewMode = 'card';
      fixture.detectChanges();
      
      const button = compiled.query(By.css('[data-testid="market-view-toggle"]'));
      expect(button.nativeElement.textContent).toContain('List');
    });

    it('should have toggle button with correct label in list mode', () => {
      component.marketViewMode = 'list';
      fixture.detectChanges();
      
      const button = compiled.query(By.css('[data-testid="market-view-toggle"]'));
      expect(button.nativeElement.textContent).toContain('Card');
    });

    it('should toggle view when button is clicked', () => {
      fixture.detectChanges();
      const button = compiled.query(By.css('[data-testid="market-view-toggle"]'));
      
      button.nativeElement.click();
      fixture.detectChanges();
      
      expect(component.marketViewMode).toBe('list');
      expect(component.isListView()).toBe(true);
    });

    it('should display all market data in card view', () => {
      component.marketViewMode = 'card';
      fixture.detectChanges();
      
      const cards = compiled.queryAll(
        By.css('[data-testid="market-cards"] > div')
      );
      expect(cards.length).toBe(component.marketData.length);
    });

    it('should display all market data in list view', () => {
      component.marketViewMode = 'list';
      fixture.detectChanges();
      
      const rows = compiled.queryAll(
        By.css('[data-testid="market-list"] tbody tr')
      );
      expect(rows.length).toBe(component.marketData.length);
    });
  });
});
