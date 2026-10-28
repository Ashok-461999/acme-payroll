import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';

import { countryName, departmentLabel } from '../../core/constants/reference-data';
import type {
  AnalyticsSummary,
  CountryPayrollSummary,
  CurrencyPayrollSummary,
  DepartmentPayrollSummary
} from '../../core/models/analytics.model';
import { MoneyPipe } from '../../core/pipes/money.pipe';
import { AnalyticsService } from '../../core/services/analytics.service';
import { apiErrorMessage } from '../../core/utils/api-error';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    RouterLink,
    MoneyPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  summary: AnalyticsSummary | null = null;
  selectedCurrency = '';
  loading = false;
  errorMessage = '';

  constructor(private readonly analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.analyticsService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        const stillExists = summary.currencies.some(
          (currency) => currency.currency === this.selectedCurrency
        );
        if (!stillExists) {
          const usd = summary.currencies.find((currency) => currency.currency === 'USD');
          this.selectedCurrency = usd?.currency ?? summary.currencies[0]?.currency ?? '';
        }
        this.loading = false;
      },
      error: (error: unknown) => {
        this.loading = false;
        this.summary = null;
        this.errorMessage = apiErrorMessage(error, 'Could not load payroll analytics.');
      }
    });
  }

  selectCurrency(currency: string): void {
    this.selectedCurrency = currency;
  }

  get selectedSummary(): CurrencyPayrollSummary | null {
    return (
      this.summary?.currencies.find((row) => row.currency === this.selectedCurrency) ?? null
    );
  }

  get departmentRows(): DepartmentPayrollSummary[] {
    return [...(this.summary?.departments ?? [])]
      .filter((row) => row.currency === this.selectedCurrency)
      .sort((left, right) => right.totalSalary - left.totalSalary);
  }

  get countryRows(): CountryPayrollSummary[] {
    return [...(this.summary?.countries ?? [])]
      .filter((row) => row.currency === this.selectedCurrency)
      .sort(
        (left, right) =>
          right.headcount - left.headcount || right.totalSalary - left.totalSalary
      );
  }

  get maxDepartmentAverage(): number {
    return Math.max(1, ...this.departmentRows.map((row) => row.averageSalary));
  }

  departmentBarWidth(averageSalary: number): string {
    const percentage = Math.max(4, (averageSalary / this.maxDepartmentAverage) * 100);
    return `${percentage.toFixed(1)}%`;
  }

  departmentName(department: DepartmentPayrollSummary['department']): string {
    return departmentLabel(department);
  }

  countryLabel(code: string): string {
    return countryName(code);
  }
}
