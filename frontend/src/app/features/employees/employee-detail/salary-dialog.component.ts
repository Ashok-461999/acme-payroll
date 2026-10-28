import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { SALARY_REASONS } from '../../../core/constants/reference-data';
import type {
  Employee,
  SalaryChangeRequest,
  SalaryRecord,
  SalaryReason
} from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../core/pipes/money.pipe';
import { EmployeeService } from '../../../core/services/employee.service';
import { apiErrorMessage, apiFieldErrors } from '../../../core/utils/api-error';

export interface SalaryDialogData {
  employee: Employee;
}

@Component({
  selector: 'app-salary-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MoneyPipe,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './salary-dialog.component.html',
  styleUrl: './salary-dialog.component.scss'
})
export class SalaryDialogComponent {
  readonly reasons = SALARY_REASONS;
  readonly today = new Date();
  readonly currentEffectiveDate: Date;
  readonly minimumAllowedDate: Date;
  readonly noValidDate: boolean;

  saving = false;
  generalError = '';

  readonly form = new FormGroup({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)]
    }),
    effectiveDate: new FormControl<Date | null>(null, {
      validators: [Validators.required]
    }),
    reason: new FormControl<SalaryReason>('ANNUAL_REVIEW', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    note: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(255)]
    })
  });

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly dialogRef: MatDialogRef<SalaryDialogComponent, SalaryRecord>,
    @Inject(MAT_DIALOG_DATA) readonly data: SalaryDialogData
  ) {
    this.currentEffectiveDate = this.parseDate(data.employee.currentSalaryEffectiveDate);
    this.minimumAllowedDate = this.addDays(this.currentEffectiveDate, 1);
    this.noValidDate = this.minimumAllowedDate > this.today;

    if (!this.noValidDate) {
      this.form.controls.effectiveDate.setValue(this.minimumAllowedDate);
    }

    this.form.controls.amount.valueChanges.subscribe(() => {
      const control = this.form.controls.amount;
      if (control.hasError('server')) {
        const errors = { ...(control.errors ?? {}) };
        delete errors['server'];
        control.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }
    });
  }

  get amount() {
    return this.form.controls.amount;
  }

  get effectiveDate() {
    return this.form.controls.effectiveDate;
  }

  get reason() {
    return this.form.controls.reason;
  }

  get note() {
    return this.form.controls.note;
  }

  get proposedChange(): number | null {
    const amount = this.amount.value;
    if (amount === null || Number.isNaN(amount)) {
      return null;
    }
    return amount - this.data.employee.currentSalary;
  }

  get changePercent(): number | null {
    const change = this.proposedChange;
    if (change === null || this.data.employee.currentSalary <= 0) {
      return null;
    }
    return (change / this.data.employee.currentSalary) * 100;
  }

  submit(): void {
    this.generalError = '';
    this.clearServerErrors();
    this.validateDates();

    if (this.noValidDate || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    const values = this.form.getRawValue();
    const request: SalaryChangeRequest = {
      amount: values.amount as number,
      effectiveDate: this.formatDate(values.effectiveDate),
      reason: values.reason,
      note: values.note.trim() || null
    };

    this.employeeService.changeSalary(this.data.employee.id, request).subscribe({
      next: (record) => {
        this.saving = false;
        this.dialogRef.close(record);
      },
      error: (error: unknown) => {
        this.saving = false;
        this.handleSaveError(error);
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  fieldError(name: string): string | null {
    return this.form.get(name)?.getError('server') ?? null;
  }

  private validateDates(): void {
    const effectiveDate = this.effectiveDate.value;
    if (!effectiveDate) {
      return;
    }

    const effectiveDay = this.startOfDay(effectiveDate);
    if (effectiveDay <= this.currentEffectiveDate) {
      this.effectiveDate.setErrors({
        ...(this.effectiveDate.errors ?? {}),
        afterCurrent: true
      });
    } else if (effectiveDay > this.startOfDay(this.today)) {
      this.effectiveDate.setErrors({
        ...(this.effectiveDate.errors ?? {}),
        futureDate: true
      });
    }
  }

  private handleSaveError(error: unknown): void {
    const fieldErrors = apiFieldErrors(error);
    let mapped = 0;
    for (const [field, message] of Object.entries(fieldErrors)) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ ...(control.errors ?? {}), server: message });
        control.markAsTouched();
        mapped += 1;
      }
    }

    this.generalError =
      mapped > 0
        ? 'Some fields need attention. Review the messages below and try again.'
        : apiErrorMessage(error, 'Could not record this salary change. Please try again.');
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls)) {
      if (control.hasError('server')) {
        const errors = { ...(control.errors ?? {}) };
        delete errors['server'];
        control.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }
    }
  }

  private parseDate(value: string): Date {
    return new Date(`${value}T00:00:00`);
  }

  private addDays(date: Date, days: number): Date {
    const copy = new Date(date);
    copy.setDate(copy.getDate() + days);
    return copy;
  }

  private startOfDay(date: Date): Date {
    const copy = new Date(date);
    copy.setHours(0, 0, 0, 0);
    return copy;
  }

  private formatDate(date: Date | null): string {
    if (!date) {
      return '';
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
