import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private static readonly formatters = new Map<number, Intl.NumberFormat>();

  transform(
    value: number | string | null | undefined,
    currency: string | null | undefined,
    fractionDigits = 0
  ): string {
    if (value === null || value === undefined || value === '' || !currency) {
      return '';
    }

    const amount = typeof value === 'number' ? value : Number(value);
    if (!Number.isFinite(amount)) {
      return '';
    }

    const digits = Math.min(2, Math.max(0, Math.trunc(fractionDigits)));
    let formatter = MoneyPipe.formatters.get(digits);
    if (!formatter) {
      formatter = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
      });
      MoneyPipe.formatters.set(digits, formatter);
    }

    return `${currency.toUpperCase()} ${formatter.format(amount)}`;
  }
}
