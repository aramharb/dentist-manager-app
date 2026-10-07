import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InvitationPreview, InvitationService } from '../admin/admin-user.service';

const ROLE_LABELS: Record<string, string> = {
  doctor: 'Doctor',
  secretaire: 'Secretary',
  manager: 'Cabinet manager',
  admin: 'Administrator',
};

/** Public page opened from a one-time invitation link: the person chooses their own password. */
@Component({
  selector: 'app-invite',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="invite-page">
      <section class="invite-card">
        <p class="eyebrow">Dentist Management System</p>

        <ng-container *ngIf="state() === 'loading'"><p>Checking your invitation…</p></ng-container>

        <ng-container *ngIf="state() === 'invalid'">
          <h1>This link is no longer valid</h1>
          <p>It may have expired or already been used. Ask your administrator or cabinet manager for a new link.</p>
          <a routerLink="/login" class="primary">Go to sign in</a>
        </ng-container>

        <ng-container *ngIf="state() === 'ready' && preview() as info">
          <h1>Welcome, {{ info.fullName }}</h1>
          <p>
            Choose a password for your {{ roleLabel(info.role) }} account
            <span *ngIf="info.cabinetName">at <strong>{{ info.cabinetName }}</strong></span>.
            You will sign in with the username <strong class="mono">{{ info.username }}</strong>.
          </p>
          <form (submit)="$event.preventDefault(); submit()">
            <label>
              Password
              <input type="password" name="password" [(ngModel)]="password" autocomplete="new-password" required />
            </label>
            <label>
              Confirm password
              <input type="password" name="confirm" [(ngModel)]="confirm" autocomplete="new-password" required />
            </label>
            <p class="error" *ngIf="error()" role="alert">{{ error() }}</p>
            <button class="primary" type="submit" [disabled]="saving()">
              {{ saving() ? 'Saving…' : 'Set my password' }}
            </button>
          </form>
        </ng-container>

        <ng-container *ngIf="state() === 'done'">
          <h1>Password saved</h1>
          <p>Your account is ready. You can now sign in.</p>
          <a routerLink="/login" class="primary">Sign in</a>
        </ng-container>
      </section>
    </main>
  `,
  styles: [
    `
      .invite-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: var(--bg); }
      .invite-card { width: min(100%, 440px); padding: 32px; border: 1px solid var(--line); border-radius: 24px; background: var(--surface-solid); box-shadow: var(--shadow); }
      .eyebrow { margin: 0 0 8px; color: var(--primary); font-size: 12px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; }
      h1 { margin: 0 0 8px; font-size: 24px; }
      p { color: var(--muted); line-height: 1.5; }
      form { display: grid; gap: 14px; margin-top: 16px; }
      label { display: grid; gap: 6px; font-weight: 600; font-size: 14px; }
      input { padding: 12px 14px; border: 1px solid var(--line); border-radius: 12px; font: inherit; }
      .mono { font-family: ui-monospace, monospace; }
      .error { margin: 0; color: var(--danger); font-size: 14px; }
      .primary { display: inline-block; padding: 12px 20px; border: 0; border-radius: 14px; color: white; font: inherit; font-weight: 700; text-align: center; text-decoration: none; cursor: pointer; background: linear-gradient(135deg, var(--primary), var(--cyan)); }
      .primary:disabled { opacity: 0.6; cursor: default; }
    `,
  ],
})
export class InviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly invitations = inject(InvitationService);

  readonly state = signal<'loading' | 'ready' | 'invalid' | 'done'>('loading');
  readonly preview = signal<InvitationPreview | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  password = '';
  confirm = '';
  private token = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.invitations.preview(this.token).subscribe({
      next: (preview) => {
        this.preview.set(preview);
        this.state.set('ready');
      },
      error: () => this.state.set('invalid'),
    });
  }

  roleLabel(role: string): string {
    return ROLE_LABELS[role] ?? role;
  }

  submit(): void {
    if (this.password.length < 6) return this.error.set('Password must be at least 6 characters.');
    if (this.password !== this.confirm) return this.error.set('The two passwords do not match.');
    this.error.set('');
    this.saving.set(true);
    this.invitations.accept(this.token, this.password).subscribe({
      next: () => {
        this.saving.set(false);
        this.state.set('done');
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        if (error.status === 404) this.state.set('invalid');
        else this.error.set(error.error?.messages?.join?.(' ') ?? 'Something went wrong. Please try again.');
      },
    });
  }
}
