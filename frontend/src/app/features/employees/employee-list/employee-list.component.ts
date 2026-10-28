import { CommonModule } from '@angular/common';
import { Component, DestroyRef } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, type PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  COUNTRIES,
  CURRENCIES,
  DEPARTMENTS,
  countryName,
  departmentLabel
} from '../../../core/constants/reference-data';
import type { Department, Employee } from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../core/pipes/money.pipe';
import { EmployeeService } from '../../../core/services/employee.service';
import { apiErrorMessage } from '../../../core/utils/api-error';

const ALLOWED_SORT_FIELDS = [
  'employeeCode',
  'firstName',
  'lastName',
  'department',
  'country',
  'currency',
  'hireDate',
  'currentSalary'
];

@Component({
  selector: 'app-employee-list',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MoneyPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss'
})
export class EmployeeListComponent {
  readonly departments = DEPARTMENTS;
  readonly countries = COUNTRIES;
  readonly currencies = CURRENCIES;
  readonly filterForm = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    department: new FormControl<Department | ''>('', { nonNullable: true }),
    country: new FormControl('', { nonNullable: true }),
    currency: new FormControl('', { nonNullable: true })
  });

  employees: Employee[] = [];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  sortBy = 'lastName';
  direction: 'asc' | 'desc' = 'asc';
  loading = false;
  errorMessage = '';

  private requestId = 0;

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly destroyRef: DestroyRef
  ) {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.pageIndex = this.parsePage(params.get('page'));
        this.pageSize = this.parsePageSize(params.get('size'));
        this.sortBy = this.parseSortBy(params.get('sortBy'));
        this.direction = params.get('direction') === 'desc' ? 'desc' : 'asc';

        this.filterForm.patchValue(
          {
            search: params.get('search') ?? '',
            department: this.parseDepartment(params.get('department')),
            country: params.get('country') ?? '',
            currency: params.get('currency') ?? ''
          },
          { emitEvent: false }
        );

        this.loadEmployees();
      });

    this.filterForm.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(
          (previous, current) => JSON.stringify(previous) === JSON.stringify(current)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.applyFilters());
  }

  get hasActiveFilters(): boolean {
    const values = this.filterForm.getRawValue();
    return Boolean(values.search || values.department || values.country || values.currency);
  }

  loadEmployees(): void {
    const currentRequest = ++this.requestId;
    this.loading = true;
    this.errorMessage = '';
    const filters = this.filterForm.getRawValue();

    this.employeeService
      .list({
        search: filters.search.trim() || undefined,
        department: filters.department || undefined,
        country: filters.country || undefined,
        currency: filters.currency || undefined,
        page: this.pageIndex,
        size: this.pageSize,
        sortBy: this.sortBy,
        direction: this.direction
      })
      .subscribe({
        next: (page) => {
          if (currentRequest !== this.requestId) {
            return;
          }
          this.employees = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: (error: unknown) => {
          if (currentRequest !== this.requestId) {
            return;
          }
          this.employees = [];
          this.totalElements = 0;
          this.loading = false;
          this.errorMessage = apiErrorMessage(error, 'Could not load employees.');
        }
      });
  }

  clearSearch(): void {
    this.filterForm.controls.search.setValue('');
  }

  clearFilters(): void {
    this.filterForm.reset(
      { search: '', department: '', country: '', currency: '' },
      { emitEvent: false }
    );
    this.navigateWithState(0);
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.navigateWithState(this.pageIndex);
  }

  openEmployee(employee: Employee): void {
    void this.router.navigate(['/employees', employee.id]);
  }

  changeSort(field: string): void {
    if (this.sortBy === field) {
      this.direction = this.direction === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.direction = field === 'currentSalary' || field === 'hireDate' ? 'desc' : 'asc';
    }
    this.navigateWithState(0);
  }

  sortIcon(field: string): string {
    if (this.sortBy !== field) {
      return 'unfold_more';
    }
    return this.direction === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  initials(employee: Employee): string {
    return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`.toUpperCase();
  }

  departmentName(department: Department): string {
    return departmentLabel(department);
  }

  countryLabel(code: string): string {
    return countryName(code);
  }

  private applyFilters(): void {
    this.navigateWithState(0);
  }

  private navigateWithState(page: number): void {
    const filters = this.filterForm.getRawValue();
    const queryParams: Record<string, string | number> = {
      page,
      size: this.pageSize,
      sortBy: this.sortBy,
      direction: this.direction
    };

    if (filters.search.trim()) {
      queryParams['search'] = filters.search.trim();
    }
    if (filters.department) {
      queryParams['department'] = filters.department;
    }
    if (filters.country) {
      queryParams['country'] = filters.country;
    }
    if (filters.currency) {
      queryParams['currency'] = filters.currency;
    }

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true
    });
  }

  private parsePage(value: string | null): number {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
  }

  private parsePageSize(value: string | null): number {
    const parsed = Number(value);
    return [10, 20, 50, 100].includes(parsed) ? parsed : 20;
  }

  private parseSortBy(value: string | null): string {
    return value && ALLOWED_SORT_FIELDS.includes(value) ? value : 'lastName';
  }

  private parseDepartment(value: string | null): Department | '' {
    return DEPARTMENTS.some((department) => department.value === value)
      ? (value as Department)
      : '';
  }
}
