import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Input, PLATFORM_ID, inject, signal } from '@angular/core';
import { Invitation } from '../../admin/admin-user.service';

/** Shows the one-time invitation link of an account, ready to copy and send. */
@Component({
  selector: 'app-invitation-link',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="invitation" *ngIf="invitation">
      <p>
        Send this link to <strong>{{ invitation.fullName }}</strong> ({{ invitation.username }}). They open it once to
        choose their own password. It expires on {{ invitation.expiresAt | date: 'medium' }} and shows only now.
      </p>
      <div class="link-row">
        <input readonly [value]="url" aria-label="Invitation link" (focus)="$any($event.target).select()" />
        <button type="button" (click)="copy()">{{ copied() ? 'Copied' : 'Copy' }}</button>
      </div>
    </div>
  `,
  styles: [
    `
      .invitation p { margin: 0 0 12px; color: var(--muted); font-size: 14px; line-height: 1.5; }
      .link-row { display: flex; gap: 8px; }
      input { flex: 1; min-width: 0; padding: 10px 12px; border: 1px solid var(--line); border-radius: 12px; font: inherit; font-size: 13px; background: #f7fbff; }
      button { padding: 10px 16px; border: 0; border-radius: 12px; color: white; font: inherit; font-weight: 700; cursor: pointer; background: linear-gradient(135deg, var(--primary), var(--cyan)); }
    `,
  ],
})
export class InvitationLinkComponent {
  private readonly platformId = inject(PLATFORM_ID);

  @Input() invitation: Invitation | null = null;
  readonly copied = signal(false);

  get url(): string {
    const origin = isPlatformBrowser(this.platformId) ? window.location.origin : '';
    return `${origin}${this.invitation?.path ?? ''}`;
  }

  copy(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    void navigator.clipboard?.writeText(this.url).then(() => {
      this.copied.set(true);
      window.setTimeout(() => this.copied.set(false), 2000);
    });
  }
}
