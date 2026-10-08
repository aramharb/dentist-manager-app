import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { CabinetBranding } from '../../client/api/client-api.service';
import { SessionService } from '../auth/session.service';

/**
 * Gives the staff workspace the colour of the user's cabinet. The CSS of the app reads --primary, so
 * setting it on the document root restyles buttons, accents and gradients without touching components.
 */
@Injectable({ providedIn: 'root' })
export class CabinetThemeService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private appliedFor: number | null = null;

  start(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.session.currentUser$.subscribe((user) => {
      const cabinetId = user?.cabinetId ?? null;
      if (cabinetId === this.appliedFor) return;
      this.appliedFor = cabinetId;
      if (cabinetId === null) return this.reset();
      this.http.get<CabinetBranding>(`/api/public/cabinets/${cabinetId}`).subscribe({
        next: (branding) => this.apply(branding.primaryColor),
        error: () => this.reset(),
      });
    });
  }

  /** Applies a colour right away (used after the manager changes the cabinet colour). */
  use(hex: string): void {
    this.apply(hex);
  }

  private apply(hex: string): void {
    if (!/^#[0-9A-Fa-f]{6}$/.test(hex)) return this.reset();
    const root = this.document.documentElement.style;
    root.setProperty('--primary', hex);
    root.setProperty('--primary-dark', this.darken(hex, 0.78));
  }

  private reset(): void {
    const root = this.document.documentElement.style;
    root.removeProperty('--primary');
    root.removeProperty('--primary-dark');
  }

  private darken(hex: string, factor: number): string {
    const parts = [1, 3, 5].map((i) => Math.round(parseInt(hex.slice(i, i + 2), 16) * factor));
    return `#${parts.map((part) => part.toString(16).padStart(2, '0')).join('')}`;
  }
}
