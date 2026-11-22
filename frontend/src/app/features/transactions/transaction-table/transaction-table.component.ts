import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap, tap } from 'rxjs';

import { Page } from '../../../core/models/page.model';
import { BankTransaction } from '../../../core/models/transaction.model';
import { TransactionService } from '../../../core/services/transaction.service';
import { toErrorMessage } from '../../../core/utils/http-error';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { TransactionFilterStore } from '../transaction-filter.store';

const EMPTY_PAGE: Page<BankTransaction> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

/**
 * Server-side paged / sorted table. The FILTER comes from the shared TransactionFilterStore;
 * paging and sorting are local to the table.
 *
 * Pattern: signals -> one `computed` query -> `toObservable` -> debounce + switchMap
 * (cancels the previous request) -> `toSignal` for the template.
 */
@Component({
  selector: 'app-transaction-table',
  standalone: true,
  imports: [DatePipe, InrPipe, MatPaginatorModule, MatProgressBarModule, MatSortModule, MatTableModule],
  templateUrl: './transaction-table.component.html',
  styleUrl: './transaction-table.component.scss',
})
export class TransactionTableComponent {
  private readonly service = inject(TransactionService);
  private readonly store = inject(TransactionFilterStore);

  readonly columns = ['transactedAt', 'details', 'type', 'amount', 'transactionId'];

  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly sortActive = signal('transactedAt');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    // Whenever the filter changes, go back to page 1 (page 5 of the old result may not exist).
    // `untracked` = don't make this effect depend on pageIndex; allowSignalWrites is required in Angular 17.
    effect(
      () => {
        this.store.filter();
        untracked(() => this.pageIndex.set(0));
      },
      { allowSignalWrites: true },
    );
  }

  private readonly query = computed(() => ({
    ...this.store.query(),
    page: {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortActive()},${this.sortDirection()}`,
    },
  }));

  readonly page = toSignal(
    toObservable(this.query).pipe(
      debounceTime(250),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => {
        this.loading.set(true);
        this.error.set(null);
      }),
      switchMap(({ filter, page }) =>
        this.service.search(filter, page).pipe(
          catchError((err: HttpErrorResponse) => {
            this.error.set(toErrorMessage(err));
            return of(EMPTY_PAGE);
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: EMPTY_PAGE },
  );

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  onSort(sort: Sort): void {
    this.sortActive.set(sort.active);
    this.sortDirection.set(sort.direction === 'asc' ? 'asc' : 'desc');
    this.pageIndex.set(0);
  }
}
