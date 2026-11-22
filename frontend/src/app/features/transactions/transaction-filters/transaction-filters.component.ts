import { Component, computed, inject, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipListboxChange, MatChipsModule } from '@angular/material/chips';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { FinancialYear, TransactionType } from '../../../core/models/transaction.model';
import { parseIsoDate, toIsoDate } from '../../../core/utils/format';
import { AMOUNT_RANGES, AmountRange, TransactionFilterStore } from '../transaction-filter.store';

/**
 * Filter bar: search, type toggle, date range picker, quick chips (financial years, amount ranges)
 * and removable chips for every active filter. It only WRITES to the store; it loads no data itself.
 */
@Component({
  selector: 'app-transaction-filters',
  standalone: true,
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule, // DateAdapter for the datepicker, based on native JS Date
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './transaction-filters.component.html',
  styleUrl: './transaction-filters.component.scss',
})
export class TransactionFiltersComponent {
  readonly store = inject(TransactionFilterStore);

  /** Financial years that contain data (from GET /transactions/periods). */
  readonly periods = input<FinancialYear[]>([]);

  readonly amountRanges = AMOUNT_RANGES;
  readonly fromDate = computed(() => parseIsoDate(this.store.from()));
  readonly toDate = computed(() => parseIsoDate(this.store.to()));

  onSearch(value: string): void {
    this.store.search.set(value);
  }

  onType(value: TransactionType | null): void {
    this.store.type.set(value);
  }

  onFromDate(date: Date | null): void {
    this.store.from.set(toIsoDate(date));
  }

  onToDate(date: Date | null): void {
    this.store.to.set(toIsoDate(date));
  }

  /**
   * Chip listbox change. `event.value` is the selected option's value, or undefined when the user
   * deselects the active chip - then we clear that filter.
   */
  onPeriod(event: MatChipListboxChange): void {
    const fy = event.value as FinancialYear | undefined;
    this.store.setDateRange(fy?.from ?? null, fy?.to ?? null);
  }

  onAmount(event: MatChipListboxChange): void {
    const range = event.value as AmountRange | undefined;
    this.store.setAmountRange(range?.min ?? null, range?.max ?? null);
  }
}
