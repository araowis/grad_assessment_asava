import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ExchangeService } from './exchange';
import { OrderRequest, OrderResponse } from '../models/exchange';

describe('ExchangeService', () => {
  let service: ExchangeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExchangeService]
    });

    service = TestBed.inject(ExchangeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call placeOrder endpoint', () => {
    const request: OrderRequest = { /* fill required fields for your model */ } as any;
    const response: OrderResponse = { /* fill required fields for your model */ } as any;

    service.placeOrder(request).subscribe((res) => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne('/api/v1/exchange/orders');
    expect(req.request.method).toBe('POST');
    req.flush(response);
  });
});
