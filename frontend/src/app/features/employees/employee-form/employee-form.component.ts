import { CommonModule, formatDate } from '@angular/common';
import { Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  COUNTRIES,
  CURRENCIES,
  DEPARTMENTS,
  currencyForCountry
} from '../../../core/constants/reference-data';
import type { Department, Employee } from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../core/pipes/money.pipe';
import { EmployeeService } from '../../../core/services/employee.service';
import { apiErrorMessage, apiFieldErrors } from '../../../core/utils/api-error';

@Component({
  selector: 'app-employee-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MoneyPipe,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss'
})
export class EmployeeFormComponent implements OnInit {
  readonly departments = DEPARTMENTS;
  readonly countries = COUNTRIES;
  readonly currencies = CURRENCIES;
  readonly today = new Date();

  mode: 'create' | 'edit' = 'create';
  employee: Employee | null = null;
  employeeId: number | null = null;
  loading = false;
  saving = false;
  loadError = '';
  generalError = '';

  readonly form = new FormGroup({
    employeeCode: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(20),
        Validators.pattern(/^[A-Za-z0-9-]+$/)
      ]
    }),
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)]
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)]
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)]
    }),
    department: new FormControl<Department>('ENGINEERING', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    jobTitle: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)]
    }),
    country: new FormControl('US', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    currency: new FormControl('USD', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    hireDate: new FormControl<Date | null>(null, { validators: [Validators.required] }),
    initialSalary: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)]
    }),
    initialSalaryEffectiveDate: new FormControl<Date | null>(new Date(), {
      validators: [Validators.required]
    })
  });

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly destroyRef: DestroyRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.mode = 'edit';
      this.employeeId = Number(idParam);
      this.setupEditMode();
      this.loadEmployee();
    } else {
      this.mode = 'create';
      this.form.controls.employeeCode.setValue(this.generateEmployeeCode());
      this.initialSalaryEffectiveDate.setValue(this.today);

      this.form.controls.country.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((country) => {
          const suggested = currencyForCountry(country);
          if (suggested) {
            this.form.controls.currency.setValue(suggested);
          }
        });

      this.form.controls.hireDate.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((hireDate) => {
          const effectiveDate = this.initialSalaryEffectiveDate.value;
          if (hireDate && (!effectiveDate || effectiveDate < hireDate)) {
            this.initialSalaryEffectiveDate.setValue(hireDate);
          }
        });
    }
  }

  get title(): string {
    return this.mode === 'create' ? 'Add employee' : 'Edit employee';
  }

  get employeeCode() {
    return this.form.controls.employeeCode;
  }

  get firstName() {
    return this.form.controls.firstName;
  }

  get lastName() {
    return this.form.controls.lastName;
  }

  get email() {
    return this.form.controls.email;
  }

  get department() {
    return this.form.controls.department;
  }

  get jobTitle() {
    return this.form.controls.jobTitle;
  }

  get country() {
    return this.form.controls.country;
  }

  get currency() {
    return this.form.controls.currency;
  }

  get hireDate() {
    return this.form.controls.hireDate;
  }

  get initialSalary() {
    return this.form.controls.initialSalary;
  }

  get initialSalaryEffectiveDate() {
    return this.form.controls.initialSalaryEffectiveDate;
  }

  regenerateEmployeeCode(): void {
    this.employeeCode.setValue(this.generateEmployeeCode());
  }

  submit(): void {
    this.generalError = '';
    this.clearServerErrors();
    this.validateEffectiveDate();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    if (this.mode === 'create') {
      this.createEmployee();
    } else {
      this.updateEmployee();
    }
  }

  cancel(): void {
    if (this.mode === 'edit' && this.employeeId !== null) {
      void this.router.navigate(['/employees', this.employeeId]);
    } else {
      void this.router.navigate(['/employees']);
    }
  }

  fieldError(name: string): string | null {
    return this.form.get(name)?.getError('server') ?? null;
  }

  private createEmployee(): void {
    const values = this.form.getRawValue();
    this.employeeService
      .create({
        employeeCode: values.employeeCode.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        department: values.department,
        jobTitle: values.jobTitle.trim(),
        country: values.country,
        currency: values.currency,
        hireDate: this.formatDate(values.hireDate),
        initialSalary: values.initialSalary as number,
        initialSalaryEffectiveDate: this.formatDate(values.initialSalaryEffectiveDate)
      })
      .subscribe({
        next: (employee) => {
          this.saving = false;
          this.snackBar.open(
            `${employee.fullName} was added with employee code ${employee.employeeCode}.`,
            'Dismiss',
            { duration: 5000 }
          );
          void this.router.navigate(['/employees', employee.id]);
        },
        error: (error: unknown) => {
          this.saving = false;
          this.handleSaveError(error);
        }
      });
  }

  private updateEmployee(): void {
    if (this.employeeId === null) {
      return;
    }

    const values = this.form.getRawValue();
    this.employeeService
      .update(this.employeeId, {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        department: values.department,
        jobTitle: values.jobTitle.trim(),
        country: values.country
      })
      .subscribe({
        next: (employee) => {
          this.saving = false;
          this.snackBar.open(`${employee.fullName}'s details were updated.`, 'Dismiss', {
            duration: 4000
          });
          void this.router.navigate(['/employees', employee.id]);
        },
        error: (error: unknown) => {
          this.saving = false;
          this.handleSaveError(error);
        }
      });
  }

  private loadEmployee(): void {
    if (this.employeeId === null) {
      return;
    }

    this.loading = true;
    this.loadError = '';
    this.form.disable();

    this.employeeService.getById(this.employeeId).subscribe({
      next: (employee) => {
        this.employee = employee;
        this.form.patchValue({
          employeeCode: employee.employeeCode,
          firstName: employee.firstName,
          lastName: employee.lastName,
          email: employee.email,
          department: employee.department,
          jobTitle: employee.jobTitle,
          country: employee.country,
          currency: employee.currency,
          hireDate: this.parseDate(employee.hireDate),
          initialSalary: employee.currentSalary,
          initialSalaryEffectiveDate: this.parseDate(employee.currentSalaryEffectiveDate)
        });
        this.form.enable();
        this.loading = false;
      },
      error: (error: unknown) => {
        this.form.enable();
        this.loading = false;
        this.loadError = apiErrorMessage(error, 'Could not load this employee.');
      }
    });
  }

  private setupEditMode(): void {
    this.form.controls.employeeCode.disable();
    this.form.controls.initialSalary.clearValidators();
    this.form.controls.initialSalary.updateValueAndValidity();
    this.form.controls.initialSalaryEffectiveDate.clearValidators();
    this.form.controls.initialSalaryEffectiveDate.updateValueAndValidity();
  }

  private validateEffectiveDate(): void {
    if (this.mode !== 'create') {
      return;
    }

    const hireDate = this.hireDate.value;
    const effectiveDate = this.initialSalaryEffectiveDate.value;
    if (hireDate && effectiveDate && effectiveDate < hireDate) {
      this.initialSalaryEffectiveDate.setErrors({
        ...(this.initialSalaryEffectiveDate.errors ?? {}),
        beforeHireDate: true
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
        : apiErrorMessage(error, 'Could not save this employee. Please try again.');
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

  private formatDate(date: Date | null): string {
    return date ? formatDate(date, 'yyyy-MM-dd', 'en-US') : '';
  }

  private parseDate(value: string): Date {
    return new Date(`${value}T00:00:00`);
  }

  private generateEmployeeCode(): string {
    const random = Math.floor(100000 + Math.random() * 900000);
    return `EMP-${random}`;
  }
}
