import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs';

import { CountItem, ProjectProgress, Summary } from '../../core/models/summary.model';
import { SummaryService } from '../../core/services/summary.service';

interface StatCard {
  label: string;
  value: number;
  icon: string;
  tone: 'primary' | 'success' | 'warn' | 'neutral';
  hint?: string;
}

/**
 * Dashboard page: loads GET /api/v1/summary and renders it.
 * State is held in signals (Angular 17); the HTTP call stays an Observable.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly summaryService = inject(SummaryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly summary = signal<Summary | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly projectColumns = ['name', 'tasks', 'progress'];

  /** Derived state: recomputed automatically when `summary` changes. */
  readonly statCards = computed<StatCard[]>(() => {
    const s = this.summary();
    if (!s) return [];
    return [
      { label: 'Projects', value: s.totalProjects, icon: 'folder', tone: 'primary' },
      { label: 'Total Tasks', value: s.totalTasks, icon: 'assignment', tone: 'neutral' },
      {
        label: 'Completed',
        value: s.completedTasks,
        icon: 'task_alt',
        tone: 'success',
        hint: `${s.completionRate}% completion`,
      },
      { label: 'Overdue', value: s.overdueTasks, icon: 'schedule', tone: 'warn', hint: 'Past due & not done' },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.summaryService
      .getSummary()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef), // auto-unsubscribe when the component is destroyed
      )
      .subscribe({
        next: (data) => this.summary.set(data),
        error: (err: HttpErrorResponse) => this.error.set(this.toMessage(err)),
      });
  }

  /** Width (0-100) of a breakdown bar relative to all tasks. */
  share(item: CountItem): number {
    const total = this.summary()?.totalTasks ?? 0;
    return total === 0 ? 0 : (item.count / total) * 100;
  }

  trackProject(_: number, p: ProjectProgress): number {
    return p.id;
  }

  private toMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Cannot reach the API. Is the backend container running?';
    // Backend returns RFC 7807 ProblemDetail: { title, status, detail }
    return err.error?.detail ?? `Request failed (${err.status} ${err.statusText})`;
  }
}
