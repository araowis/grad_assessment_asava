import { TestBed } from '@angular/core/testing';

import { StockStreamService } from './stock-stream';

describe('StockStream', () => {
  let service: StockStreamService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StockStreamService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
