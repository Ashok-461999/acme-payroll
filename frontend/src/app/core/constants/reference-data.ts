import type { Department, SalaryReason } from '../models/employee.model';

export interface DepartmentOption {
  value: Department;
  label: string;
}

export interface CountryOption {
  code: string;
  name: string;
  defaultCurrency: string;
}

export interface CurrencyOption {
  code: string;
  name: string;
}

export interface SalaryReasonOption {
  value: SalaryReason;
  label: string;
}

export const DEPARTMENTS: DepartmentOption[] = [
  { value: 'ENGINEERING', label: 'Engineering' },
  { value: 'PRODUCT', label: 'Product' },
  { value: 'DESIGN', label: 'Design' },
  { value: 'SALES', label: 'Sales' },
  { value: 'MARKETING', label: 'Marketing' },
  { value: 'FINANCE', label: 'Finance' },
  { value: 'HUMAN_RESOURCES', label: 'Human Resources' },
  { value: 'OPERATIONS', label: 'Operations' },
  { value: 'CUSTOMER_SUCCESS', label: 'Customer Success' },
  { value: 'LEGAL', label: 'Legal' }
];

export const COUNTRIES: CountryOption[] = [
  { code: 'US', name: 'United States', defaultCurrency: 'USD' },
  { code: 'IN', name: 'India', defaultCurrency: 'INR' },
  { code: 'GB', name: 'United Kingdom', defaultCurrency: 'GBP' },
  { code: 'DE', name: 'Germany', defaultCurrency: 'EUR' },
  { code: 'CA', name: 'Canada', defaultCurrency: 'CAD' },
  { code: 'AU', name: 'Australia', defaultCurrency: 'AUD' },
  { code: 'SG', name: 'Singapore', defaultCurrency: 'SGD' },
  { code: 'BR', name: 'Brazil', defaultCurrency: 'BRL' },
  { code: 'JP', name: 'Japan', defaultCurrency: 'JPY' },
  { code: 'ZA', name: 'South Africa', defaultCurrency: 'ZAR' },
  { code: 'FR', name: 'France', defaultCurrency: 'EUR' },
  { code: 'NL', name: 'Netherlands', defaultCurrency: 'EUR' },
  { code: 'IE', name: 'Ireland', defaultCurrency: 'EUR' },
  { code: 'ES', name: 'Spain', defaultCurrency: 'EUR' },
  { code: 'IT', name: 'Italy', defaultCurrency: 'EUR' },
  { code: 'SE', name: 'Sweden', defaultCurrency: 'SEK' },
  { code: 'CH', name: 'Switzerland', defaultCurrency: 'CHF' },
  { code: 'AE', name: 'United Arab Emirates', defaultCurrency: 'AED' },
  { code: 'NZ', name: 'New Zealand', defaultCurrency: 'NZD' },
  { code: 'MX', name: 'Mexico', defaultCurrency: 'MXN' }
];

export const CURRENCIES: CurrencyOption[] = [
  { code: 'USD', name: 'US Dollar' },
  { code: 'EUR', name: 'Euro' },
  { code: 'GBP', name: 'British Pound' },
  { code: 'INR', name: 'Indian Rupee' },
  { code: 'CAD', name: 'Canadian Dollar' },
  { code: 'AUD', name: 'Australian Dollar' },
  { code: 'SGD', name: 'Singapore Dollar' },
  { code: 'BRL', name: 'Brazilian Real' },
  { code: 'JPY', name: 'Japanese Yen' },
  { code: 'ZAR', name: 'South African Rand' },
  { code: 'CHF', name: 'Swiss Franc' },
  { code: 'SEK', name: 'Swedish Krona' },
  { code: 'AED', name: 'UAE Dirham' },
  { code: 'NZD', name: 'New Zealand Dollar' },
  { code: 'MXN', name: 'Mexican Peso' }
];

export const SALARY_REASONS: SalaryReasonOption[] = [
  { value: 'ANNUAL_REVIEW', label: 'Annual review' },
  { value: 'PROMOTION', label: 'Promotion' },
  { value: 'MARKET_ADJUSTMENT', label: 'Market adjustment' },
  { value: 'OTHER', label: 'Other' }
];

export function departmentLabel(value: Department): string {
  return DEPARTMENTS.find((department) => department.value === value)?.label ?? value;
}

export function countryName(code: string): string {
  return COUNTRIES.find((country) => country.code === code)?.name ?? code;
}

export function currencyForCountry(code: string): string | null {
  return COUNTRIES.find((country) => country.code === code)?.defaultCurrency ?? null;
}

export function salaryReasonLabel(value: SalaryReason): string {
  if (value === 'INITIAL') {
    return 'Initial salary';
  }
  return SALARY_REASONS.find((reason) => reason.value === value)?.label ?? value;
}
