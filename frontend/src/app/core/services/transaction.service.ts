import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Page } from '../models/page.model';
import {
  BankTransaction,
  Counterparty,
  EMPTY_FILTER,
  FinancialYear,
  ImportResult,
  ImportSummary,
  PageRequest,
  TransactionFilter,
  TransactionStats,
} from '../models/transaction.model';

/** All calls for the transactions feature (backend TransactionController + TransactionImportController). */
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/transactions`;

  search(filter: TransactionFilter, page: PageRequest): Observable<Page<BankTransaction>> {
    const params = toParams(filter).set('page', page.page).set('size', page.size).set('sort', page.sort);
    return this.http.get<Page<BankTransaction>>(this.baseUrl, { params });
  }

  /** KPIs for the filtered set; no argument = all transactions. */
  stats(filter: TransactionFilter = EMPTY_FILTER): Observable<TransactionStats> {
    return this.http.get<TransactionStats>(`${this.baseUrl}/stats`, { params: toParams(filter) });
  }

  counterparties(filter: TransactionFilter, limit = 8): Observable<Counterparty[]> {
    const params = toParams(filter).set('limit', limit);
    return this.http.get<Counterparty[]>(`${this.baseUrl}/counterparties`, { params });
  }

  periods(): Observable<FinancialYear[]> {
    return this.http.get<FinancialYear[]>(`${this.baseUrl}/periods`);
  }

  /**
   * multipart/form-data upload. `observe: 'events'` + `reportProgress` emit UploadProgress events
   * (for the progress bar) and finally the HttpResponse with the ImportResult body.
   * Don't set Content-Type yourself - the browser adds the multipart boundary.
   */
  upload(file: File): Observable<HttpEvent<ImportResult>> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ImportResult>(`${this.baseUrl}/imports`, form, {
      observe: 'events',
      reportProgress: true,
    });
  }

  recentImports(): Observable<ImportSummary[]> {
    return this.http.get<ImportSummary[]>(`${this.baseUrl}/imports`);
  }
}

/** Only non-empty values become query params, matching the backend's "null = no condition". */
function toParams(filter: TransactionFilter): HttpParams {
  let params = new HttpParams();
  if (filter.type) params = params.set('type', filter.type);
  if (filter.q.trim()) params = params.set('q', filter.q.trim());
  if (filter.from) params = params.set('from', filter.from);
  if (filter.to) params = params.set('to', filter.to);
  if (filter.minAmount !== null) params = params.set('minAmount', filter.minAmount);
  if (filter.maxAmount !== null) params = params.set('maxAmount', filter.maxAmount);
  return params;
}
