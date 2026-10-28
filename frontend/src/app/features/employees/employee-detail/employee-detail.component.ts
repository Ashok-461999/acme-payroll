import { CommonModule } from '@angular/common';
import { Component, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  countryName,
  departmentLabel,
  salaryReasonLabel
} from '../../../core/constants/reference-data';
import type { Employee, SalaryRecord } from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../core/pipes/money.pipe';
import { EmployeeService } from '../../../core/services/employee.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import {
  SalaryDialogComponent,
  type SalaryDialogData
} from './salary-dialog.component';

interface SalaryHistoryRow {
  id: number;
  amount: number;
  currency: string;
  effectiveDate: string;
  reason: SalaryRecord['reason'];
  note: string | null;
  createdAt: string;
  change: number | null;
  changePercent: number | null;
}

@Component({
  selector: 'app-employee-detail',
  imports: [
    CommonModule,
    RouterLink,
    MoneyPipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss'
})
export class EmployeeDetailComponent {
  employeeId = 0;
  employee: Employee | null = null;
  history: SalaryRecord[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly destroyRef: DestroyRef
  ) {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = Number(params.get('id'));
      if (!Number.isInteger(id) || id <= 0) {
        void this.router.navigate(['/employees']);
        return;
      }

      this.employeeId = id;
      this.employee = null;
      this.history = [];
      this.load(true);
    });
  }

  get salaryRows(): SalaryHistoryRow[] {
    return this.history.map((record, index) => {
      const previous = this.history[index + 1];
      const change = previous ? record.amount - previous.amount : null;
      const changePercent =
        previous && previous.amount > 0 ? (change! / previous.amount) * 100 : null;

      return {
        id: record.id,
        amount: record.amount,
        currency: record.currency,
        effectiveDate: record.effectiveDate,
        reason: record.reason,
        note: record.note,
        createdAt: record.createdAt,
        change,
        changePercent
      };
    });
  }

  load(showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
    }
    this.errorMessage = '';

    forkJoin({
      employee: this.employeeService.getById(this.employeeId),
      history: this.employeeService.history(this.employeeId)
    }).subscribe({
      next: ({ employee, history }) => {
        this.employee = employee;
        this.history = history;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.loading = false;
        this.errorMessage = apiErrorMessage(error, 'Could not load this employee profile.');
      }
    });
  }

  openSalaryDialog(): void {
    if (!this.employee) {
      return;
    }

    const dialogRef = this.dialog.open<
      SalaryDialogComponent,
      SalaryDialogData,
      SalaryRecord
    >(SalaryDialogComponent, {
      width: '560px',
      maxWidth: '94vw',
      data: { employee: this.employee },
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });

    dialogRef.afterClosed().subscribe((record) => {
      if (!record) {
        return;
      }

      this.snackBar.open(
        `Salary updated to ${record.currency} ${record.amount.toLocaleString()} effective ${record.effectiveDate}.`,
        'Dismiss',
        { duration: 5000 }
      );
      this.load(false);
    });
  }

  initials(employee: Employee): string {
    return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`.toUpperCase();
  }

  departmentName(employee: Employee): string {
    return departmentLabel(employee.department);
  }

  countryLabel(code: string): string {
    return countryName(code);
  }

  reasonLabel(row: SalaryHistoryRow): string {
    return salaryReasonLabel(row.reason);
  }

  changeClass(row: SalaryHistoryRow): 'increase' | 'decrease' | 'neutral' {
    if (row.change === null || row.change === 0) {
      return 'neutral';
    }
    return row.change > 0 ? 'increase' : 'decrease';
  }
}
