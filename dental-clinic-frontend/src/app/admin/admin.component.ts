import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { SessionLifecycleService } from '../core/auth/session-lifecycle.service';
import { SessionService, UserRole } from '../core/auth/session.service';
import { LanguageSwitcherComponent } from '../core/i18n/language-switcher.component';
import { NotificationService } from '../core/notifications/notification.service';
import { ModalComponent } from '../secretaire/shared/modal/modal.component';
import { AdminUser, AdminUserService } from './admin-user.service';

type DialogMode = 'create' | 'edit' | 'password';

interface UserForm {
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  password: string;
  confirmPassword: string;
}

const ROLE_LABELS: Record<UserRole, string> = {
  doctor: 'Doctor',
  secretaire: 'Secretary',
  admin: 'Admin',
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, LanguageSwitcherComponent, ModalComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  private readonly users = inject(AdminUserService);
  private readonly session = inject(SessionService);
  private readonly lifecycle = inject(SessionLifecycleService);
  private readonly notifications = inject(NotificationService);

  readonly roles = Object.entries(ROLE_LABELS) as [UserRole, string][];
  readonly roleLabels = ROLE_LABELS;
  readonly currentUser = this.session.currentUser;

  readonly accounts = signal<AdminUser[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly filter = signal('');
  readonly showInactive = signal(true);

  readonly visibleAccounts = computed(() => {
    const query = this.filter().trim().toLowerCase();
    return this.accounts().filter(
      (user) =>
        (this.showInactive() || user.active) &&
        (!query || `${user.fullName} ${user.username} ${ROLE_LABELS[user.role]}`.toLowerCase().includes(query)),
    );
  });

  readonly stats = computed(() => {
    const all = this.accounts();
    return {
      total: all.length,
      active: all.filter((user) => user.active).length,
      doctors: all.filter((user) => user.role === 'doctor' && user.active).length,
      secretaries: all.filter((user) => user.role === 'secretaire' && user.active).length,
    };
  });

  dialogMode: DialogMode | null = null;
  selected: AdminUser | null = null;
  form: UserForm = this.emptyForm();
  formError = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.users.list().subscribe({
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

  isSelf(user: AdminUser): boolean {
    return user.id === this.currentUser?.id;
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
    this.openDialog('create');
  }

  openEdit(user: AdminUser): void {
    this.selected = user;
    this.form = { ...this.emptyForm(), username: user.username, fullName: user.fullName, role: user.role, active: user.active };
    this.openDialog('edit');
  }

  openPassword(user: AdminUser): void {
    this.selected = user;
    this.form = this.emptyForm();
    this.openDialog('password');
  }

  closeDialog(): void {
    if (this.saving()) return;
    this.dialogMode = null;
    this.selected = null;
  }

  get dialogTitle(): string {
    if (this.dialogMode === 'create') return 'New account';
    if (this.dialogMode === 'password') return `Reset password: ${this.selected?.fullName ?? ''}`;
    return `Edit ${this.selected?.fullName ?? 'account'}`;
  }

  toggleActive(user: AdminUser): void {
    this.save(
      this.users.update(user.id, { username: user.username, fullName: user.fullName, role: user.role, active: !user.active }),
      user.active ? `${user.fullName} was deactivated.` : `${user.fullName} was reactivated.`,
    );
  }

  submit(): void {
    this.formError = this.validate();
    if (this.formError) return;

    const { username, fullName, role, active, password } = this.form;
    if (this.dialogMode === 'create') {
      this.save(this.users.create({ username, fullName, role, password }), `${fullName} can now sign in.`);
    } else if (this.dialogMode === 'edit' && this.selected) {
      this.save(this.users.update(this.selected.id, { username, fullName, role, active }), `${fullName} was updated.`);
    } else if (this.dialogMode === 'password' && this.selected) {
      this.save(this.users.resetPassword(this.selected.id, password), `Password changed for ${this.selected.fullName}.`);
    }
  }

  logout(): void {
    this.lifecycle.logout();
  }

  private save(request: Observable<unknown>, successMessage: string): void {
    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogMode = null;
        this.selected = null;
        this.notify('Saved', successMessage);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        const message = this.errorMessage(error);
        if (this.dialogMode) this.formError = message;
        else this.notify('Change not saved', message);
      },
    });
  }

  private validate(): string {
    const { username, fullName, password, confirmPassword } = this.form;
    if (this.dialogMode !== 'password') {
      if (!/^[a-zA-Z0-9._-]{3,80}$/.test(username.trim())) {
        return 'Username must be 3-80 characters: letters, digits, dot, dash or underscore.';
      }
      if (!fullName.trim()) return 'Full name is required.';
    }
    if (this.dialogMode !== 'edit') {
      if (password.length < 6) return 'Password must be at least 6 characters.';
      if (password !== confirmPassword) return 'The two passwords do not match.';
    }
    return '';
  }

  private openDialog(mode: DialogMode): void {
    this.formError = '';
    this.dialogMode = mode;
  }

  private emptyForm(): UserForm {
    return { username: '', fullName: '', role: 'secretaire', active: true, password: '', confirmPassword: '' };
  }

  private notify(title: string, message: string): void {
    this.notifications.show({ kind: 'system', title, message });
  }

  private errorMessage(error: HttpErrorResponse): string {
    const messages = error.error?.messages;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.status === 403) return 'Only an admin can manage accounts.';
    if (error.status === 0) return 'The server is unreachable.';
    return 'Something went wrong. Please try again.';
  }
}
