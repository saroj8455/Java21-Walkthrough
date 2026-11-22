/**
 * Indian formatting helpers. Intl.NumberFormat('en-IN') gives lakh grouping (₹1,18,800) without
 * registering Angular locale data.
 */
const INR_WHOLE = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const INR_EXACT = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const DATE = new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

export function formatInr(value: number, exact = false): string {
  return (exact ? INR_EXACT : INR_WHOLE).format(value);
}

/** "2022-04-01" -> local Date. (new Date('2022-04-01') would be UTC midnight and can show the previous day.) */
export function parseIsoDate(iso: string | null): Date | null {
  if (!iso) return null;
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

/** Local Date -> "2022-04-01". (toISOString() converts to UTC and can shift the day in IST.) */
export function toIsoDate(date: Date | null): string | null {
  if (!date) return null;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function formatIsoDate(iso: string | null): string {
  const date = parseIsoDate(iso);
  return date ? DATE.format(date) : '';
}
