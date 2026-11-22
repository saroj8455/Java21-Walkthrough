import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Summary } from '../models/summary.model';

/** Talks to GET /api/v1/summary (backend SummaryController). */
@Injectable({ providedIn: 'root' })
export class SummaryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/summary`;

  getSummary(): Observable<Summary> {
    return this.http.get<Summary>(this.baseUrl);
  }
}
