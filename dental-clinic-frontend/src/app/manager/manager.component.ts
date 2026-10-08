import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { AdminUser, Invitation, ManagerUserService } from '../admin/admin-user.service';
import { SessionLifecycleService } from '../core/auth/session-lifecycle.service';
import { SessionService, UserRole } from '../core/auth/session.service';
import { LanguageSwitcherComponent } from '../core/i18n/language-switcher.component';
import { NotificationService } from '../core/notifications/notification.service';
import { ModalComponent } from '../secretaire/shared/modal/modal.component';
import { InvitationLinkComponent } from '../shared/invitation-link/invitation-link.component';
import { BrandingEditorComponent } from '../branding/branding-editor.component';

type DialogMode = 'create' | 'edit';

interface MemberForm {
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
}

const ROLE_LABELS: Partial<Record<UserRole, string>> = { doctor: 'Doctor', secretaire: 'Secretary' };

/** Workspace of the cabinet manager: creates and manages the doctors and secretaries of their own cabinet. */
@Component({
  selector: 'app-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, LanguageSwitcherComponent, ModalComponent, InvitationLinkComponent, BrandingEditorComponent],
  templateUrl: './manager.component.html',
  styleUrl: '../admin/admin.component.css',
})
export class ManagerComponent implements OnInit {
  private readonly members = inject(ManagerUserService);
  private readonly session = inject(SessionService);
  private readonly lifecycle = inject(SessionLifecycleService);
  private readonly notifications = inject(NotificationService);

  readonly currentUser = this.session.currentUser;
  readonly roles = [
    ['secretaire', 'Secretary'],
    ['doctor', 'Doctor'],
  ] as [UserRole, string][];
  readonly roleLabels = ROLE_LABELS;

  readonly accounts = signal<AdminUser[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly view = signal<'team' | 'identity'>('team');
  readonly filter = signal('');
  readonly showInactive = signal(true);

  readonly visibleAccounts = computed(() => {
    const query = this.filter().trim().toLowerCase();
    return this.accounts().filter(
      (user) =>
        (this.showInactive() || user.active) &&
        (!query || `${user.fullName} ${user.username} ${ROLE_LABELS[user.role] ?? ''}`.toLowerCase().includes(query)),
    );
  });

  readonly stats = computed(() => {
    const active = this.accounts().filter((user) => user.active);
    return {
      total: this.accounts().length,
      doctors: active.filter((user) => user.role === 'doctor').length,
      secretaries: active.filter((user) => user.role === 'secretaire').length,
      invited: active.filter((user) => user.invitationPending).length,
    };
  });

  dialogMode: DialogMode | null = null;
  selected: AdminUser | null = null;
  form: MemberForm = this.emptyForm();
  formError = '';
  invitation: Invitation | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.members.list().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.notify('Could not load accounts', this.errorMessage(error));
      },
    });
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  openCreate(): void {
    this.selected = null;
    this.form = this.emptyForm();
    this.formError = '';
    this.dialogMode = 'create';
  }

  openEdit(user: AdminUser): void {
    this.selected = user;
    this.form = { username: user.username, fullName: user.fullName, role: user.role, active: user.active };
    this.formError = '';
    this.dialogMode = 'edit';
  }

  closeDialog(): void {
    if (this.saving()) return;
    this.dialogMode = null;
    this.selected = null;
  }

  closeInvitation(): void {
    this.invitation = null;
  }

  get dialogTitle(): string {
    return this.dialogMode === 'create' ? 'New account' : `Edit ${this.selected?.fullName ?? 'account'}`;
  }

  toggleActive(user: AdminUser): void {
    this.saving.set(true);
    this.members
      .update(user.id, { username: user.username, fullName: user.fullName, role: user.role, active: !user.active })
      .subscribe(this.observer(user.active ? `${user.fullName} was deactivated.` : `${user.fullName} was reactivated.`));
  }

  /** A fresh link: first password for a new person, or a new password for someone who forgot theirs. */
  sendInvitation(user: AdminUser): void {
    this.saving.set(true);
    this.members.invite(user.id).subscribe({
      next: (invitation) => {
        this.saving.set(false);
        this.invitation = invitation;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notify('Link not created', this.errorMessage(error));
      },
    });
  }

  submit(): void {
    this.formError = this.validate();
    if (this.formError) return;
    const { username, fullName, role, active } = this.form;
    this.saving.set(true);
    if (this.dialogMode === 'create') {
      this.members.create({ username, fullName, role }).subscribe({
        next: (created) => {
          this.saving.set(false);
          this.dialogMode = null;
          this.invitation = created.invitation;
          this.load();
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
    } else if (this.selected) {
      this.members
        .update(this.selected.id, { username, fullName, role, active })
        .subscribe(this.observer(`${fullName} was updated.`));
    }
  }

  logout(): void {
    this.lifecycle.logout();
  }

  private observer(successMessage: string) {
    return {
      next: () => {
        this.saving.set(false);
        this.dialogMode = null;
        this.selected = null;
        this.notify('Saved', successMessage);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    };
  }

  private fail(error: HttpErrorResponse): void {
    this.saving.set(false);
    const message = this.errorMessage(error);
    if (this.dialogMode) this.formError = message;
    else this.notify('Change not saved', message);
  }

  private validate(): string {
    const { username, fullName } = this.form;
    if (!/^[a-zA-Z0-9._-]{3,80}$/.test(username.trim())) {
      return 'Username must be 3-80 characters: letters, digits, dot, dash or underscore.';
    }
    if (!fullName.trim()) return 'Full name is required.';
    return '';
  }

  private emptyForm(): MemberForm {
    return { username: '', fullName: '', role: 'secretaire', active: true };
  }

  private notify(title: string, message: string): void {
    this.notifications.show({ kind: 'system', title, message });
  }

  private errorMessage(error: HttpErrorResponse): string {
    const messages = error.error?.messages;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.status === 403) return 'Only the cabinet manager can manage these accounts.';
    if (error.status === 0) return 'The server is unreachable.';
    return 'Something went wrong. Please try again.';
  }
}
