import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import type { Employee } from '../models/employee.model';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let http: HttpTestingController;

  const employee: Employee = {
    id: 1,
    employeeCode: 'EMP-10001',
    firstName: 'Ada',
    lastName: 'Lovelace',
    fullName: 'Ada Lovelace',
    email: 'ada@acme.test',
    department: 'ENGINEERING',
    jobTitle: 'Staff Engineer',
    country: 'GB',
    currency: 'GBP',
    hireDate: '2021-04-12',
    currentSalary: 92000,
    currentSalaryEffectiveDate: '2025-04-01',
    createdAt: '2021-04-12T09:00:00Z',
    updatedAt: '2025-04-01T09:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(EmployeeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends list filters, pagination and sorting as query parameters', () => {
    service
      .list({
        search: 'ada',
        department: 'ENGINEERING',
        country: 'GB',
        currency: 'GBP',
        page: 2,
        size: 50,
        sortBy: 'currentSalary',
        direction: 'desc'
      })
      .subscribe();

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('ada');
    expect(request.request.params.get('department')).toBe('ENGINEERING');
    expect(request.request.params.get('country')).toBe('GB');
    expect(request.request.params.get('currency')).toBe('GBP');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sortBy')).toBe('currentSalary');
    expect(request.request.params.get('direction')).toBe('desc');
    request.flush({
      content: [employee],
      page: 2,
      size: 50,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    });
  });

  it('omits empty filters from the query string', () => {
    service
      .list({ page: 0, size: 20, sortBy: 'lastName', direction: 'asc' })
      .subscribe();

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.params.has('search')).toBeFalse();
    expect(request.request.params.has('department')).toBeFalse();
    expect(request.request.params.has('country')).toBeFalse();
    expect(request.request.params.has('currency')).toBeFalse();
    request.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    });
  });

  it('retrieves one employee and their salary history', () => {
    service.getById(1).subscribe((result) => expect(result).toEqual(employee));
    const employeeRequest = http.expectOne('/api/employees/1');
    expect(employeeRequest.request.method).toBe('GET');
    employeeRequest.flush(employee);

    service.history(1).subscribe((result) => expect(result.length).toBe(1));
    const historyRequest = http.expectOne('/api/employees/1/salaries');
    expect(historyRequest.request.method).toBe('GET');
    historyRequest.flush([
      {
        id: 10,
        amount: 92000,
        currency: 'GBP',
        effectiveDate: '2025-04-01',
        reason: 'ANNUAL_REVIEW',
        note: 'Promotion cycle',
        createdAt: '2025-04-01T09:00:00Z'
      }
    ]);
  });

  it('creates an employee and posts salary changes to the employee history endpoint', () => {
    const createRequest = {
      employeeCode: 'EMP-20001',
      firstName: 'Grace',
      lastName: 'Hopper',
      email: 'grace@acme.test',
      department: 'ENGINEERING' as const,
      jobTitle: 'Principal Engineer',
      country: 'US',
      currency: 'USD',
      hireDate: '2024-01-08',
      initialSalary: 145000,
      initialSalaryEffectiveDate: '2024-01-08'
    };

    service.create(createRequest).subscribe((result) => expect(result).toEqual(employee));
    const post = http.expectOne('/api/employees');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(createRequest);
    post.flush(employee);

    service
      .changeSalary(1, {
        amount: 95000,
        effectiveDate: '2026-01-01',
        reason: 'PROMOTION',
        note: null
      })
      .subscribe();
    const salaryPost = http.expectOne('/api/employees/1/salaries');
    expect(salaryPost.request.method).toBe('POST');
    expect(salaryPost.request.body.reason).toBe('PROMOTION');
    salaryPost.flush({
      id: 11,
      amount: 95000,
      currency: 'GBP',
      effectiveDate: '2026-01-01',
      reason: 'PROMOTION',
      note: null,
      createdAt: '2026-01-01T09:00:00Z'
    });
  });
});
