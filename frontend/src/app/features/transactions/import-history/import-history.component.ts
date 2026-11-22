import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ImportSummary } from '../../../core/models/transaction.model';

/** Presentational ("dumb") component: renders the list it receives, no service calls. */
@Component({
  selector: 'app-import-history',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule],
  template: `
    @if (imports().length === 0) {
      <p class="text-muted small mb-0">No uploads yet.</p>
    } @else {
      <div class="table-scroll">
        <table mat-table [dataSource]="imports()" class="w-100">
          <ng-container matColumnDef="startedAt">
            <th mat-header-cell *matHeaderCellDef>When</th>
            <td mat-cell *matCellDef="let i" class="text-nowrap">{{ i.startedAt | date: 'dd MMM, HH:mm:ss' }}</td>
          </ng-container>
          <ng-container matColumnDef="fileName">
            <th mat-header-cell *matHeaderCellDef>File</th>
            <td mat-cell *matCellDef="let i">{{ i.fileName }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let i">
              <span class="status" [class.failed]="i.status === 'FAILED'" [matTooltip]="i.errorMessage ?? ''">
                {{ i.status }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="counts">
            <th mat-header-cell *matHeaderCellDef>Imported / Duplicates / Invalid</th>
            <td mat-cell *matCellDef="let i">{{ i.importedRows }} / {{ i.duplicateRows }} / {{ i.invalidRows }}</td>
          </ng-container>
          <ng-container matColumnDef="download">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let i" class="text-end">
              @if (i.invalidCsvUrl) {
                <a mat-icon-button [href]="i.invalidCsvUrl" download="invalid_csv_statements.csv"
                   matTooltip="Download invalid_csv_statements.csv">
                  <mat-icon>download</mat-icon>
                </a>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </div>
    }
  `,
  styles: [
    `
      .table-scroll { max-height: 320px; overflow: auto; }
      .status { font-size: 0.8rem; padding: 2px 8px; border-radius: 10px; background: #e8f5e9; color: #1b5e20; }
      .status.failed { background: #ffebee; color: #b71c1c; }
    `,
  ],
})
export class ImportHistoryComponent {
  readonly imports = input.required<ImportSummary[]>();
  readonly columns = ['startedAt', 'fileName', 'status', 'counts', 'download'];
}
