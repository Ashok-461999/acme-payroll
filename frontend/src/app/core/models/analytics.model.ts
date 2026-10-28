import type { Department } from './employee.model';

export interface CurrencyPayrollSummary {
  currency: string;
  headcount: number;
  totalSalary: number;
  averageSalary: number;
  minSalary: number;
  maxSalary: number;
}

export interface DepartmentPayrollSummary {
  currency: string;
  department: Department;
  headcount: number;
  totalSalary: number;
  averageSalary: number;
}

export interface CountryPayrollSummary {
  currency: string;
  country: string;
  headcount: number;
  totalSalary: number;
  averageSalary: number;
}

export interface AnalyticsSummary {
  totalEmployees: number;
  currencies: CurrencyPayrollSummary[];
  departments: DepartmentPayrollSummary[];
  countries: CountryPayrollSummary[];
}
