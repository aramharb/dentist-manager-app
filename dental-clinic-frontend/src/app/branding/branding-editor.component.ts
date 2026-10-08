import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CabinetBranding, cabinetImageUrl } from '../client/api/client-api.service';
import { NotificationService } from '../core/notifications/notification.service';
import { CabinetThemeService } from '../core/theme/cabinet-theme.service';

/**
 * Edits the identity of a cabinet: owner name, tagline, colour, contact details, logo and cover photo.
 * The manager edits their own cabinet; the admin passes a {@code cabinetId} to edit any cabinet.
 */
@Component({
  selector: 'app-branding-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="editor" *ngIf="branding() as b">
      <section class="preview" [style.--accent]="form.primaryColor">
        <div class="cover" [style.background-image]="b.hasCover ? 'url(' + imageUrl(b, 'cover') + ')' : null"></div>
        <div class="body">
          <img *ngIf="b.hasLogo" class="logo" [src]="imageUrl(b, 'logo')" alt="Logo" />
          <span *ngIf="!b.hasLogo" class="logo fallback">{{ initials(b.name) }}</span>
          <h3>{{ b.name }}</h3>
          <p class="owner" *ngIf="form.ownerName">{{ form.ownerName }}</p>
          <p class="tagline" *ngIf="form.tagline">{{ form.tagline }}</p>
          <p class="meta" *ngIf="form.address">{{ form.address }}</p>
        </div>
      </section>

      <section class="pictures">
        <div>
          <strong>Logo</strong>
          <small>Carré, JPG / PNG / WebP, 2 Mo max.</small>
          <div class="row">
            <label class="button">Choisir<input type="file" accept="image/png,image/jpeg,image/webp" hidden (change)="upload('logo', $event)" /></label>
            <button type="button" class="ghost" *ngIf="b.hasLogo" (click)="remove('logo')">Retirer</button>
          </div>
        </div>
        <div>
          <strong>Photo de couverture</strong>
          <small>Format large, JPG / PNG / WebP, 2 Mo max.</small>
          <div class="row">
            <label class="button">Choisir<input type="file" accept="image/png,image/jpeg,image/webp" hidden (change)="upload('cover', $event)" /></label>
            <button type="button" class="ghost" *ngIf="b.hasCover" (click)="remove('cover')">Retirer</button>
          </div>
        </div>
      </section>

      <form class="fields" (submit)="$event.preventDefault(); save()">
        <label>Nom du propriétaire / médecin responsable
          <input name="ownerName" [(ngModel)]="form.ownerName" maxlength="160" placeholder="Dr. Nom Prénom" />
        </label>
        <label>Slogan
          <input name="tagline" [(ngModel)]="form.tagline" maxlength="200" placeholder="Votre sourire, notre métier" />
        </label>
        <label>Couleur du cabinet
          <span class="color-row">
            <input name="colorPicker" type="color" [(ngModel)]="form.primaryColor" />
            <input name="color" [(ngModel)]="form.primaryColor" maxlength="7" class="hex" />
          </span>
        </label>
        <label>Adresse<input name="address" [(ngModel)]="form.address" maxlength="255" /></label>
        <label>Téléphone<input name="phone" [(ngModel)]="form.phoneNumber" maxlength="30" /></label>
        <label>Email<input name="email" type="email" [(ngModel)]="form.email" maxlength="150" /></label>
        <p class="error" *ngIf="error()" role="alert">{{ error() }}</p>
        <div class="actions"><button type="submit" class="primary" [disabled]="saving()">{{ saving() ? 'Enregistrement…' : 'Enregistrer' }}</button></div>
      </form>
    </div>
    <p class="muted" *ngIf="!branding() && !error()">Chargement…</p>
    <p class="error" *ngIf="!branding() && error()">{{ error() }}</p>
  `,
  styles: [
    `
      .editor { display: grid; gap: 18px; }
      .preview { overflow: hidden; border: 1px solid var(--line, #dce9f3); border-radius: 20px; background: #fff; }
      .cover { height: 90px; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent) 55%, white)) center / cover; }
      .body { position: relative; display: grid; gap: 3px; padding: 38px 18px 16px; }
      .body h3 { margin: 0; }
      .body p { margin: 0; }
      .logo { position: absolute; top: -28px; left: 18px; width: 56px; height: 56px; border: 3px solid #fff; border-radius: 16px; object-fit: cover; background: #fff; box-shadow: 0 6px 16px rgba(0,0,0,.12); }
      .logo.fallback { display: grid; place-items: center; font-weight: 800; color: #fff; background: var(--accent); }
      .owner { font-weight: 600; color: var(--accent); }
      .tagline, .meta, small, .muted { color: var(--muted, #5f7284); }
      .tagline { font-style: italic; }
      .pictures { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
      .pictures > div { display: grid; gap: 6px; padding: 14px; border: 1px solid var(--line, #dce9f3); border-radius: 16px; }
      .row { display: flex; gap: 8px; margin-top: 4px; }
      .fields { display: grid; gap: 12px; }
      label:not(.button) { display: grid; gap: 6px; font-size: 14px; font-weight: 600; }
      input:not([type='color']):not([type='file']) { padding: 10px 12px; border: 1px solid var(--line, #dce9f3); border-radius: 12px; font: inherit; }
      .color-row { display: flex; align-items: center; gap: 10px; }
      input[type='color'] { width: 48px; height: 40px; padding: 2px; border: 1px solid var(--line, #dce9f3); border-radius: 10px; background: #fff; }
      .hex { width: 110px; font-family: ui-monospace, monospace; }
      button, .button { font: inherit; font-weight: 700; cursor: pointer; border-radius: 12px; padding: 9px 16px; }
      .button { border: 1px solid var(--line, #dce9f3); background: #fff; }
      .primary { border: 0; color: #fff; background: linear-gradient(135deg, var(--primary, #1689e8), var(--cyan, #06b6d4)); }
      .ghost { border: 1px solid var(--line, #dce9f3); background: #fff; color: inherit; }
      .actions { display: flex; justify-content: flex-end; }
      .error { margin: 0; color: var(--danger, #ef4444); font-size: 14px; }
    `,
  ],
})
export class BrandingEditorComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notifications = inject(NotificationService);
  private readonly theme = inject(CabinetThemeService);

  /** Set by the admin to edit a specific cabinet; empty for the manager (own cabinet). */
  @Input() cabinetId: number | null = null;

  readonly branding = signal<CabinetBranding | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  form = { ownerName: '', tagline: '', primaryColor: '#1689E8', address: '', phoneNumber: '', email: '' };

  private get base(): string {
    return this.cabinetId == null ? '/api/manager/cabinet' : `/api/admin/cabinets/${this.cabinetId}/branding`;
  }

  ngOnInit(): void {
    this.http.get<CabinetBranding>(this.base).subscribe({
      next: (branding) => this.apply(branding),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  imageUrl(branding: CabinetBranding, kind: 'logo' | 'cover'): string {
    return cabinetImageUrl(branding, kind);
  }

  initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('');
  }

  save(): void {
    this.error.set('');
    if (!/^#[0-9A-Fa-f]{6}$/.test(this.form.primaryColor)) {
      return this.error.set('La couleur doit ressembler à #1689E8.');
    }
    this.saving.set(true);
    this.http.put<CabinetBranding>(this.base, this.payload()).subscribe({
      next: (branding) => {
        this.saving.set(false);
        this.apply(branding);
        if (this.cabinetId === null) this.theme.use(branding.primaryColor);
        this.notifications.show({ kind: 'system', title: 'Enregistré', message: 'Identité du cabinet mise à jour.' });
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.fail(error);
      },
    });
  }

  upload(kind: 'logo' | 'cover', event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) return this.error.set("L'image doit faire 2 Mo ou moins.");
    this.error.set('');
    const data = new FormData();
    data.append('file', file);
    this.http.post<CabinetBranding>(`${this.base}/images/${kind}`, data).subscribe({
      next: (branding) => this.keepTextAndApply(branding),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  remove(kind: 'logo' | 'cover'): void {
    this.http.delete<CabinetBranding>(`${this.base}/images/${kind}`).subscribe({
      next: (branding) => this.keepTextAndApply(branding),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private payload() {
    const blank = (value: string) => value.trim() || null;
    return {
      ownerName: blank(this.form.ownerName),
      tagline: blank(this.form.tagline),
      primaryColor: this.form.primaryColor.toUpperCase(),
      address: blank(this.form.address),
      phoneNumber: blank(this.form.phoneNumber),
      email: blank(this.form.email),
    };
  }

  /** After an image change the typed (unsaved) text stays as it is. */
  private keepTextAndApply(branding: CabinetBranding): void {
    this.branding.set(branding);
  }

  private apply(branding: CabinetBranding): void {
    this.branding.set(branding);
    this.form = {
      ownerName: branding.ownerName ?? '',
      tagline: branding.tagline ?? '',
      primaryColor: branding.primaryColor,
      address: branding.address ?? '',
      phoneNumber: branding.phoneNumber ?? '',
      email: branding.email ?? '',
    };
  }

  private fail(error: HttpErrorResponse): void {
    const messages = error.error?.messages;
    this.error.set(Array.isArray(messages) && messages.length ? messages.join(' ') : 'Une erreur est survenue. Réessayez.');
  }
}
