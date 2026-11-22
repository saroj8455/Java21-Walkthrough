import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';

import { Counterparty, TransactionStats } from '../../../core/models/transaction.model';
import { TransactionService } from '../../../core/services/transaction.service';
import { formatInr, formatIsoDate } from '../../../core/utils/format';
import { toErrorMessage } from '../../../core/utils/http-error';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { TransactionFilterStore } from '../transaction-filter.store';

interface Kpi {
  label: string;
  value: string;
  hint: string;
  icon: string;
  tone: 'neutral' | 'debit' | 'credit';
}

interface Insights {
  stats: TransactionStats | null;
  counterparties: Counterparty[];
}

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * KPI cards + top counterparties for exactly the rows the table shows.
 * Reloads whenever the shared filter changes (debounced, previous request cancelled by switchMap).
 */
@Component({
  selector: 'app-transaction-insights',
  standalone: true,
  imports: [InrPipe, MatCardModule, MatChipsModule, MatIconModule, MatTooltipModule],
  templateUrl: './transaction-insights.component.html',
  styleUrl: './transaction-insights.component.scss',
})
export class TransactionInsightsComponent {
  private readonly service = inject(TransactionService);
  readonly store = inject(TransactionFilterStore);

  /** All-time stats from the page, used for "x% of all" hints. */
  readonly overall = input<TransactionStats | null>(null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** forkJoin runs both requests in parallel and emits once both complete (like Promise.all). */
  private readonly insights = toSignal(
    toObservable(this.store.query).pipe(
      debounceTime(250),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => {
        this.loading.set(true);
        this.error.set(null);
      }),
      switchMap(({ filter }) =>
        forkJoin({
          stats: this.service.stats(filter),
          counterparties: this.service.counterparties(filter, 8),
        }).pipe(
          map((result): Insights => result),
          catchError((err: HttpErrorResponse) => {
            this.error.set(toErrorMessage(err));
            return of<Insights>({ stats: null, counterparties: [] });
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: { stats: null, counterparties: [] } as Insights },
  );

  readonly stats = computed(() => this.insights().stats);
  readonly counterparties = computed(() => this.insights().counterparties);

  /** Six KPI cards derived from the stats; recomputed only when stats or overall change. */
  readonly kpis = computed<Kpi[]>(() => {
    const s = this.stats();
    if (!s) return [];
    const all = this.overall();
    const share =
      this.store.isFiltered() && all?.totalCount
        ? `${((s.totalCount / all.totalCount) * 100).toFixed(1)}% of ${all.totalCount} total`
        : `${s.debitCount} debits · ${s.creditCount} credits`;

    return [
      { label: 'Transactions', value: String(s.totalCount), hint: share, icon: 'receipt_long', tone: 'neutral' },
      {
        label: 'Spent',
        value: formatInr(s.totalDebit),
        hint: `${s.debitCount} debits · avg ${formatInr(s.averageDebit)}`,
        icon: 'north_east',
        tone: 'debit',
      },
      {
        label: 'Received',
        value: formatInr(s.totalCredit),
        hint: `${s.creditCount} credits · avg ${formatInr(s.averageCredit)}`,
        icon: 'south_west',
        tone: 'credit',
      },
      {
        label: 'Net',
        value: formatInr(s.netAmount),
        hint: s.netAmount < 0 ? 'Spent more than received' : 'Received more than spent',
        icon: 'balance',
        tone: s.netAmount < 0 ? 'debit' : 'credit',
      },
      {
        label: 'Largest debit',
        value: formatInr(s.largestDebit),
        hint: `Largest credit ${formatInr(s.largestCredit)}`,
        icon: 'trending_up',
        tone: 'neutral',
      },
      { label: 'Period', value: periodValue(s), hint: periodHint(s), icon: 'date_range', tone: 'neutral' },
    ];
  });

  /** Click a counterparty chip -> filter the whole page by it (click again to remove). */
  toggleCounterparty(c: Counterparty): void {
    this.store.search.set(this.isActive(c) ? '' : c.details);
  }

  isActive(c: Counterparty): boolean {
    return this.store.search().trim() === c.details;
  }
}

function periodValue(s: TransactionStats): string {
  if (!s.firstTransactionAt || !s.lastTransactionAt) return '—';
  return `${formatIsoDate(s.firstTransactionAt)} – ${formatIsoDate(s.lastTransactionAt)}`;
}

function periodHint(s: TransactionStats): string {
  if (!s.firstTransactionAt || !s.lastTransactionAt) return 'No transactions';
  const days = Math.round((Date.parse(s.lastTransactionAt) - Date.parse(s.firstTransactionAt)) / DAY_MS) + 1;
  return `${days} day${days === 1 ? '' : 's'}`;
}
