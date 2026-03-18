import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CompanyService } from './company';
import { Company } from '../models/company';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CompanyService]
    });

    service = TestBed.inject(CompanyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call the correct endpoint when loading companies', () => {
    const mockCompanies: Company[] = [
      { shortId: 'abc', name: 'Acme', noOfShare: 100, price: 10 }
    ];

    service.getAllCompanies().subscribe((companies) => {
      expect(companies).toEqual(mockCompanies);
    });

    const req = httpMock.expectOne('/api/v1/companies');
    expect(req.request.method).toBe('GET');
    req.flush(mockCompanies);
  });
});
