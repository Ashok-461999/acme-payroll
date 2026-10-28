export type Department =
  | 'ENGINEERING'
  | 'PRODUCT'
  | 'DESIGN'
  | 'SALES'
  | 'MARKETING'
  | 'FINANCE'
  | 'HUMAN_RESOURCES'
  | 'OPERATIONS'
  | 'CUSTOMER_SUCCESS'
  | 'LEGAL';

export type SalaryReason =
  | 'INITIAL'
  | 'ANNUAL_REVIEW'
  | 'PROMOTION'
  | 'MARKET_ADJUSTMENT'
  | 'OTHER';

export interface Employee {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  department: Department;
  jobTitle: string;
  country: string;
  currency: string;
  hireDate: string;
  currentSalary: number;
  currentSalaryEffectiveDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmployeeRequest {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  department: Department;
  jobTitle: string;
  country: string;
  currency: string;
  hireDate: string;
  initialSalary: number;
  initialSalaryEffectiveDate: string;
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  department: Department;
  jobTitle: string;
  country: string;
}

export interface SalaryRecord {
  id: number;
  amount: number;
  currency: string;
  effectiveDate: string;
  reason: SalaryReason;
  note: string | null;
  createdAt: string;
}

export interface SalaryChangeRequest {
  amount: number;
  effectiveDate: string;
  reason: SalaryReason;
  note: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface EmployeeListQuery {
  search?: string;
  department?: Department;
  country?: string;
  currency?: string;
  page: number;
  size: number;
  sortBy: string;
  direction: 'asc' | 'desc';
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
