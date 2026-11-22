import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

/** App shell: Material toolbar + routed content. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" class="gap-2">
      <mat-icon>coffee</mat-icon>
      <span>Java 21 Walkthrough</span>
      <span class="flex-grow-1"></span>
      <a mat-button routerLink="/dashboard" routerLinkActive="active-link">
        <mat-icon>dashboard</mat-icon> Dashboard
      </a>
      <a mat-button routerLink="/transactions" routerLinkActive="active-link">
        <mat-icon>receipt_long</mat-icon> Transactions
      </a>
      <a mat-button href="/swagger-ui.html" target="_blank" rel="noopener">
        <mat-icon>api</mat-icon> API Docs
      </a>
    </mat-toolbar>

    <main class="container-xl py-4">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .active-link {
        background: rgba(255, 255, 255, 0.15);
      }
    `,
  ],
})
export class AppComponent {}
