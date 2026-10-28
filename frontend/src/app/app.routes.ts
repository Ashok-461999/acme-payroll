import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(
        (module) => module.DashboardComponent
      ),
    title: 'Payroll overview | ACME People'
  },
  {
    path: 'employees',
    loadComponent: () =>
      import('./features/employees/employee-list/employee-list.component').then(
        (module) => module.EmployeeListComponent
      ),
    title: 'Employees | ACME People'
  },
  {
    path: 'employees/new',
    loadComponent: () =>
      import('./features/employees/employee-form/employee-form.component').then(
        (module) => module.EmployeeFormComponent
      ),
    title: 'Add employee | ACME People'
  },
  {
    path: 'employees/:id/edit',
    loadComponent: () =>
      import('./features/employees/employee-form/employee-form.component').then(
        (module) => module.EmployeeFormComponent
      ),
    title: 'Edit employee | ACME People'
  },
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./features/employees/employee-detail/employee-detail.component').then(
        (module) => module.EmployeeDetailComponent
      ),
    title: 'Employee profile | ACME People'
  },
  { path: '**', redirectTo: 'dashboard' }
];
