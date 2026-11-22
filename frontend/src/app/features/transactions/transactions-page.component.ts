import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, forkJoin, of } from 'rxjs';

import { FinancialYear, ImportResult, ImportSummary, TransactionStats } from '../../core/models/transaction.model';
import { TransactionService } from '../../core/services/transaction.service';
import { formatIsoDate } from '../../core/utils/format';
import { ImportHistoryComponent } from './import-history/import-history.component';
import { StatementUploadComponent } from './statement-upload/statement-upload.component';
import { TransactionFilterStore } from './transaction-filter.store';
import { TransactionFiltersComponent } from './transaction-filters/transaction-filters.component';
import { TransactionInsightsComponent } from './transaction-insights/transaction-insights.component';
import { TransactionTableComponent } from './transaction-table/transaction-table.component';

/**
 * Container ("smart") component for /transactions.
 *
 *   upload --(imported)--> page --store.refresh()--> filters / insights / table reload
 *
 * `providers: [TransactionFilterStore]` creates ONE store per page instance; every child that injects
 * TransactionFilterStore gets this same instance (Angular's hierarchical injector).
 */
@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [
    MatCardModule,
    MatSnackBarModule,
    ImportHistoryComponent,
    StatementUploadComponent,
    TransactionFiltersComponent,
    TransactionInsightsComponent,
    TransactionTableComponent,
  ],
  providers: [TransactionFilterStore],
  templateUrl: './transactions-page.component.html',
})
export class TransactionsPageComponent implements OnInit {
  private readonly service = inject(TransactionService);
  private readonly store = inject(TransactionFilterStore);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  /** All-time numbers (unfiltered) - the baseline for "x% of total" in the KPI cards. */
  readonly overall = signal<TransactionStats | null>(null);
  readonly periods = signal<FinancialYear[]>([]);
  readonly imports = signal<ImportSummary[]>([]);

  readonly formatDate = formatIsoDate;

  ngOnInit(): void {
    this.loadPageData();
  }

  onImported(result: ImportResult): void {
    this.snackBar.open(
      `Imported ${result.importedRows} new, ${result.duplicateRows} duplicates, ${result.invalidRows} invalid`,
      'OK',
      { duration: 5000 },
    );
    this.store.refresh();
    this.loadPageData();
  }

  /** forkJoin = Promise.all for Observables: run the requests in parallel, continue when all finish. */
  private loadPageData(): void {
    forkJoin({
      overall: this.service.stats().pipe(catchError(() => of(null))),
      periods: this.service.periods().pipe(catchError(() => of([] as FinancialYear[]))),
      imports: this.service.recentImports().pipe(catchError(() => of([] as ImportSummary[]))),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ overall, periods, imports }) => {
        this.overall.set(overall);
        this.periods.set(periods);
        this.imports.set(imports);
      });
  }
}
