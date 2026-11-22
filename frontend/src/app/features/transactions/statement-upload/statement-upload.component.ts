import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ImportResult } from '../../../core/models/transaction.model';
import { TransactionService } from '../../../core/services/transaction.service';
import { toErrorMessage } from '../../../core/utils/http-error';

const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Drag & drop (or click) a PhonePe CSV, upload it with a progress bar, show the outcome.
 * Emits `imported` so the parent page can refresh stats, table and history.
 */
@Component({
  selector: 'app-statement-upload',
  standalone: true,
  imports: [DecimalPipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule, MatTableModule, MatTooltipModule],
  templateUrl: './statement-upload.component.html',
  styleUrl: './statement-upload.component.scss',
})
export class StatementUploadComponent {
  private readonly service = inject(TransactionService);
  private readonly destroyRef = inject(DestroyRef);

  /** Angular 17.3 signal-based output (replaces @Output() + EventEmitter). */
  readonly imported = output<ImportResult>();

  readonly file = signal<File | null>(null);
  readonly dragging = signal(false);
  readonly uploading = signal(false);
  readonly progress = signal(0);
  readonly result = signal<ImportResult | null>(null);
  readonly error = signal<string | null>(null);

  /** Bytes still being sent? After 100% the server is parsing + saving, so show an indeterminate bar. */
  readonly sending = computed(() => this.progress() < 100);
  readonly statusText = computed(() =>
    this.sending() ? `Uploading ${this.progress()}%` : 'Validating and saving rows...',
  );

  readonly previewColumns = ['lineNumber', 'reason'];

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.select(input.files?.[0] ?? null);
    input.value = ''; // allow choosing the same file again
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault(); // required, otherwise the browser opens the file
    this.dragging.set(true);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.select(event.dataTransfer?.files?.[0] ?? null);
  }

  /** Client-side checks give instant feedback; the server re-validates everything anyway. */
  select(file: File | null): void {
    this.result.set(null);
    this.error.set(null);
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.error.set('Please choose a .csv file.');
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('The file is too large (maximum 5 MB).');
      return;
    }
    this.file.set(file);
  }

  upload(): void {
    const file = this.file();
    if (!file || this.uploading()) return;

    this.uploading.set(true);
    this.progress.set(0);
    this.error.set(null);

    this.service
      .upload(file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            this.progress.set(Math.round((event.loaded / event.total) * 100));
          } else if (event.type === HttpEventType.Response && event.body) {
            this.result.set(event.body);
            this.file.set(null);
            this.uploading.set(false);
            this.imported.emit(event.body);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(toErrorMessage(err));
          this.uploading.set(false);
        },
      });
  }

  clear(): void {
    this.file.set(null);
    this.result.set(null);
    this.error.set(null);
  }
}
