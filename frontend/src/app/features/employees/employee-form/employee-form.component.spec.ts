import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import type { Employee } from '../../../core/models/employee.model';
import { EmployeeService } from '../../../core/services/employee.service';
import { EmployeeFormComponent } from './employee-form.component';

describe('EmployeeFormComponent', () => {
  let fixture: ComponentFixture<EmployeeFormComponent>;
  let component: EmployeeFormComponent;
  let employeeService: jasmine.SpyObj<EmployeeService>;
  let router: Router;
  let snackBar: MatSnackBar;

  const createdEmployee: Employee = {
    id: 1,
    employeeCode: 'EMP-30001',
    firstName: 'Katherine',
    lastName: 'Johnson',
    fullName: 'Katherine Johnson',
    email: 'katherine@acme.test',
    department: 'ENGINEERING',
    jobTitle: 'Data Analyst',
    country: 'US',
    currency: 'USD',
    hireDate: '2020-01-15',
    currentSalary: 100000,
    currentSalaryEffectiveDate: '2020-01-15',
    createdAt: '2020-01-15T09:00:00Z',
    updatedAt: '2020-01-15T09:00:00Z'
  };

  beforeEach(async () => {
    employeeService = jasmine.createSpyObj<EmployeeService>('EmployeeService', [
      'list',
      'getById',
      'create',
      'update',
      'history',
      'changeSalary'
    ]);
    employeeService.create.and.returnValue(of(createdEmployee));

    await TestBed.configureTestingModule({
      imports: [EmployeeFormComponent],
      providers: [
        provideNoopAnimations(),
        provideNativeDateAdapter(),
        { provide: EmployeeService, useValue: employeeService },
        provideRouter([])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(EmployeeFormComponent);
    snackBar = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(snackBar, 'open');
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates an employee with an initial salary and navigates to the profile', () => {
    component.form.patchValue({
      employeeCode: 'EMP-30001',
      firstName: 'Katherine',
      lastName: 'Johnson',
      email: 'katherine@acme.test',
      department: 'ENGINEERING',
      jobTitle: 'Data Analyst',
      country: 'US',
      currency: 'USD',
      hireDate: new Date(2020, 0, 15),
      initialSalary: 100000,
      initialSalaryEffectiveDate: new Date(2020, 0, 15)
    });

    component.submit();

    expect(employeeService.create).toHaveBeenCalledWith({
      employeeCode: 'EMP-30001',
      firstName: 'Katherine',
      lastName: 'Johnson',
      email: 'katherine@acme.test',
      department: 'ENGINEERING',
      jobTitle: 'Data Analyst',
      country: 'US',
      currency: 'USD',
      hireDate: '2020-01-15',
      initialSalary: 100000,
      initialSalaryEffectiveDate: '2020-01-15'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/employees', 1]);
    expect(snackBar.open).toHaveBeenCalled();
  });

  it('rejects an initial salary effective date before the hire date', () => {
    component.form.patchValue({
      employeeCode: 'EMP-30002',
      firstName: 'Katherine',
      lastName: 'Johnson',
      email: 'katherine@acme.test',
      department: 'ENGINEERING',
      jobTitle: 'Data Analyst',
      country: 'US',
      currency: 'USD',
      hireDate: new Date(2024, 0, 10),
      initialSalary: 100000,
      initialSalaryEffectiveDate: new Date(2024, 0, 9)
    });

    component.submit();

    expect(component.initialSalaryEffectiveDate.hasError('beforeHireDate')).toBeTrue();
    expect(employeeService.create).not.toHaveBeenCalled();
  });
});
