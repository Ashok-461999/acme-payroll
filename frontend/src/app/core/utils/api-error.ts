import { HttpErrorResponse } from '@angular/common/http';

import type { ApiError } from '../models/employee.model';

export function toApiError(error: unknown): ApiError | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  if (error.status === 0) {
    return {
      timestamp: new Date().toISOString(),
      status: 0,
      error: 'Network error',
      message: 'Cannot reach the payroll API. Check that the backend is running.',
      path: ''
    };
  }

  const body = error.error;
  if (body && typeof body === 'object' && typeof body.message === 'string') {
    return body as ApiError;
  }

  return null;
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  return toApiError(error)?.message ?? fallback;
}

export function apiFieldErrors(error: unknown): Record<string, string> {
  return toApiError(error)?.fieldErrors ?? {};
}
