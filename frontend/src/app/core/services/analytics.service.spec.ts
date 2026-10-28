import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AnalyticsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the payroll summary from the analytics endpoint', () => {
    const summary = {
      totalEmployees: 2,
      currencies: [
        {
          currency: 'USD',
          headcount: 2,
          totalSalary: 200000,
          averageSalary: 100000,
          minSalary: 90000,
          maxSalary: 110000
        }
      ],
      departments: [],
      countries: []
    };

    service.getSummary().subscribe((result) => expect(result).toEqual(summary));

    const request = http.expectOne('/api/analytics/summary');
    expect(request.request.method).toBe('GET');
    request.flush(summary);
  });
});
