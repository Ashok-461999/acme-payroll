import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type {
  CreateEmployeeRequest,
  Employee,
  EmployeeListQuery,
  PageResponse,
  SalaryChangeRequest,
  SalaryRecord,
  UpdateEmployeeRequest
} from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  list(query: EmployeeListQuery): Observable<PageResponse<Employee>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sortBy', query.sortBy)
      .set('direction', query.direction);

    if (query.search) {
      params = params.set('search', query.search);
    }
    if (query.department) {
      params = params.set('department', query.department);
    }
    if (query.country) {
      params = params.set('country', query.country);
    }
    if (query.currency) {
      params = params.set('currency', query.currency);
    }

    return this.http.get<PageResponse<Employee>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, request);
  }

  update(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}`, request);
  }

  history(id: number): Observable<SalaryRecord[]> {
    return this.http.get<SalaryRecord[]>(`${this.baseUrl}/${id}/salaries`);
  }

  changeSalary(id: number, request: SalaryChangeRequest): Observable<SalaryRecord> {
    return this.http.post<SalaryRecord>(`${this.baseUrl}/${id}/salaries`, request);
  }
}
