import { Injectable, computed, signal } from '@angular/core';

import { FinancialYear, TransactionFilter, TransactionType } from '../../core/models/transaction.model';
import { formatInr, formatIsoDate } from '../../core/utils/format';

export type FilterKey = 'q' | 'type' | 'date' | 'amount';

export interface FilterChip {
  key: FilterKey;
  label: string;
}

export interface AmountRange {
  label: string;
  min: number | null;
  max: number | null;
}

/** Quick-filter amount buckets. Bounds are inclusive on the API, so the upper ends stop just below the next bucket. */
export const AMOUNT_RANGES: AmountRange[] = [
  { label: 'Under ₹500', min: null, max: 499.99 },
  { label: '₹500 – ₹5k', min: 500, max: 4999.99 },
  { label: '₹5k – ₹20k', min: 5000, max: 19999.99 },
  { label: '₹20k+', min: 20000, max: null },
];

/**
 * Signal-based state shared by the filter bar, KPI cards and table of the transactions page.
 *
 * Provided in the PAGE component's `providers` (not 'root'), so each visit to the page gets a fresh
 * store and it is destroyed when you navigate away - like a NgRx ComponentStore, without the library.
 */
@Injectable()
export class TransactionFilterStore {
  readonly search = signal('');
  readonly type = signal<TransactionType | null>(null);
  readonly from = signal<string | null>(null);
  readonly to = signal<string | null>(null);
  readonly minAmount = signal<number | null>(null);
  readonly maxAmount = signal<number | null>(null);

  /** Bumped after an upload so every consumer reloads even if the filter did not change. */
  readonly refreshKey = signal(0);

  /** The filter object sent to the API (same shape as the backend TransactionFilter record). */
  readonly filter = computed<TransactionFilter>(() => ({
    q: this.search().trim(),
    type: this.type(),
    from: this.from(),
    to: this.to(),
    minAmount: this.minAmount(),
    maxAmount: this.maxAmount(),
  }));

  /** Filter + refresh trigger: what data consumers react to. */
  readonly query = computed(() => ({ filter: this.filter(), refresh: this.refreshKey() }));

  /** One removable chip per active filter. */
  readonly chips = computed<FilterChip[]>(() => {
    const f = this.filter();
    const chips: FilterChip[] = [];
    if (f.q) chips.push({ key: 'q', label: `Search: “${f.q}”` });
    if (f.type) chips.push({ key: 'type', label: f.type === 'DEBIT' ? 'Debits only' : 'Credits only' });
    if (f.from || f.to) {
      const from = f.from ? formatIsoDate(f.from) : 'start';
      const to = f.to ? formatIsoDate(f.to) : 'today';
      chips.push({ key: 'date', label: `${from} – ${to}` });
    }
    if (f.minAmount !== null || f.maxAmount !== null) {
      chips.push({ key: 'amount', label: amountLabel(f.minAmount, f.maxAmount) });
    }
    return chips;
  });

  readonly isFiltered = computed(() => this.chips().length > 0);

  remove(key: FilterKey): void {
    switch (key) {
      case 'q':
        this.search.set('');
        break;
      case 'type':
        this.type.set(null);
        break;
      case 'date':
        this.setDateRange(null, null);
        break;
      case 'amount':
        this.setAmountRange(null, null);
        break;
    }
  }

  clear(): void {
    this.search.set('');
    this.type.set(null);
    this.setDateRange(null, null);
    this.setAmountRange(null, null);
  }

  setDateRange(from: string | null, to: string | null): void {
    this.from.set(from);
    this.to.set(to);
  }

  setAmountRange(min: number | null, max: number | null): void {
    this.minAmount.set(min);
    this.maxAmount.set(max);
  }

  isFinancialYear(fy: FinancialYear): boolean {
    return this.from() === fy.from && this.to() === fy.to;
  }

  isAmountRange(range: AmountRange): boolean {
    return this.minAmount() === range.min && this.maxAmount() === range.max;
  }

  refresh(): void {
    this.refreshKey.update((k) => k + 1);
  }
}

function amountLabel(min: number | null, max: number | null): string {
  const preset = AMOUNT_RANGES.find((r) => r.min === min && r.max === max);
  if (preset) return `Amount: ${preset.label}`;
  if (min !== null && max !== null) return `Amount: ${formatInr(min)} – ${formatInr(max)}`;
  return min !== null ? `Amount ≥ ${formatInr(min)}` : `Amount ≤ ${formatInr(max ?? 0)}`;
}
