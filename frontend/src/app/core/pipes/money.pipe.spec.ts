import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('separates the currency code from the amount with a space', () => {
    expect(pipe.transform(95761, 'USD')).toBe('USD 95,761');
  });

  it('supports decimal places when requested', () => {
    expect(pipe.transform('1234.5', 'aud', 2)).toBe('AUD 1,234.50');
  });

  it('returns an empty string for missing values', () => {
    expect(pipe.transform(null, 'USD')).toBe('');
    expect(pipe.transform(1000, '')).toBe('');
  });
});
