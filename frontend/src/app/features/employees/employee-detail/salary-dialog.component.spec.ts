import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';

import type { Employee, SalaryRecord } from '../../../core/models/employee.model';
import { EmployeeService } from '../../../core/services/employee.service';
import { SalaryDialogComponent, type SalaryDialogData } from './salary-dialog.component';

describe('SalaryDialogComponent', () => {
  let employeeService: jasmine.SpyObj<EmployeeService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<SalaryDialogComponent, SalaryRecord>>;

  const employee: Employee = {
    id: 7,
    employeeCode: 'EMP-10007',
    firstName: 'Grace',
    lastName: 'Hopper',
    fullName: 'Grace Hopper',
    email: 'grace@acme.test',
    department: 'ENGINEERING',
    jobTitle: 'Principal Engineer',
    country: 'US',
    currency: 'USD',
    hireDate: '2020-01-06',
    currentSalary: 100000,
    currentSalaryEffectiveDate: todayIso(),
    createdAt: '2020-01-06T09:00:00Z',
    updatedAt: '2020-01-06T09:00:00Z'
  };

  function todayIso(): string {
    const today = new Date();
    return localIso(today);
  }

  function localIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function createComponent(data: SalaryDialogData): SalaryDialogComponent {
    TestBed.configureTestingModule({
      imports: [SalaryDialogComponent],
      providers: [
        provideNoopAnimations(),
        provideNativeDateAdapter(),
        { provide: EmployeeService, useValue: employeeService },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef }
      ]
    });

    const fixture = TestBed.createComponent(SalaryDialogComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    employeeService = jasmine.createSpyObj<EmployeeService>('EmployeeService', [
      'changeSalary'
    ]);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
  });

  it('blocks a new change when the current salary is effective today', () => {
    const component = createComponent({ employee });

    expect(component.noValidDate).toBeTrue();
    expect(component.effectiveDate.value).toBeNull();

    component.amount.setValue(110000);
    component.submit();

    expect(employeeService.changeSalary).not.toHaveBeenCalled();
  });

  it('submits a valid change using the first available effective date', () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayIso = localIso(yesterday);
    const currentEmployee: Employee = {
      ...employee,
      currentSalaryEffectiveDate: yesterdayIso
    };
    const record: SalaryRecord = {
      id: 11,
      amount: 110000,
      currency: 'USD',
      effectiveDate: todayIso(),
      reason: 'ANNUAL_REVIEW',
      note: 'Annual cycle',
      createdAt: new Date().toISOString()
    };
    employeeService.changeSalary.and.returnValue(of(record));

    const component = createComponent({ employee: currentEmployee });
    component.amount.setValue(110000);
    component.note.setValue('Annual cycle');
    component.submit();

    expect(component.noValidDate).toBeFalse();
    expect(employeeService.changeSalary).toHaveBeenCalledWith(7, {
      amount: 110000,
      effectiveDate: todayIso(),
      reason: 'ANNUAL_REVIEW',
      note: 'Annual cycle'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(record);
  });

  it('rejects an effective date that is not after the current salary date', () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayIso = localIso(yesterday);
    const currentEmployee: Employee = {
      ...employee,
      currentSalaryEffectiveDate: yesterdayIso
    };

    const component = createComponent({ employee: currentEmployee });
    component.amount.setValue(110000);
    component.effectiveDate.setValue(yesterday);
    component.submit();

    expect(component.effectiveDate.hasError('afterCurrent')).toBeTrue();
    expect(employeeService.changeSalary).not.toHaveBeenCalled();
  });
});
