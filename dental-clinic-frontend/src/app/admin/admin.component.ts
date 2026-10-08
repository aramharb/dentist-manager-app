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
import { InvitationLinkComponent } from '../shared/invitation-link/invitation-link.component';
import { BrandingEditorComponent } from '../branding/branding-editor.component';
import { AdminCabinetService, AdminUser, AdminUserService, Cabinet, Invitation } from './admin-user.service';

type DialogMode = 'create' | 'edit' | 'cabinet-create' | 'cabinet-edit' | 'manager' | 'branding';
type AdminView = 'cabinets' | 'accounts';

interface UserForm {
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  cabinetId: number | null;
}

interface CabinetForm {
  name: string;
  code: string;
  address: string;
  phoneNumber: string;
  email: string;
  active: boolean;
  copyCatalogFromCabinetId: number | null;
  managerFullName: string;
  managerUsername: string;
}

const ROLE_LABELS: Record<UserRole, string> = {
  doctor: 'Doctor',
  secretaire: 'Secretary',
  manager: 'Cabinet manager',
  admin: 'Admin',
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, LanguageSwitcherComponent, ModalComponent, InvitationLinkComponent, BrandingEditorComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  private readonly users = inject(AdminUserService);
  private readonly cabinetApi = inject(AdminCabinetService);
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
  readonly view = signal<AdminView>('cabinets');
  readonly cabinets = signal<Cabinet[]>([]);
  readonly cabinetFilter = signal<number | null>(null);

  readonly activeCabinets = computed(() => this.cabinets().filter((cabinet) => cabinet.active));
  readonly filteredCabinet = computed(
    () => this.cabinets().find((cabinet) => cabinet.id === this.cabinetFilter()) ?? null,
  );

  readonly visibleAccounts = computed(() => {
    const query = this.filter().trim().toLowerCase();
    const cabinetId = this.cabinetFilter();
    return this.accounts().filter(
      (user) =>
        (this.showInactive() || user.active) &&
        (cabinetId === null || user.cabinetId === cabinetId) &&
        (!query ||
          `${user.fullName} ${user.username} ${ROLE_LABELS[user.role]} ${user.cabinetName ?? ''}`
            .toLowerCase()
            .includes(query)),
    );
  });

  readonly cabinetTotals = computed(() => {
    const all = this.cabinets();
    return {
      total: all.length,
      active: all.filter((cabinet) => cabinet.active).length,
      doctors: all.reduce((sum, cabinet) => sum + cabinet.stats.doctors, 0),
      secretaries: all.reduce((sum, cabinet) => sum + cabinet.stats.secretaries, 0),
      patients: all.reduce((sum, cabinet) => sum + cabinet.stats.patients, 0),
    };
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
  selectedCabinet: Cabinet | null = null;
  form: UserForm = this.emptyForm();
  invitation: Invitation | null = null;
  cabinetForm: CabinetForm = this.emptyCabinetForm();
  formError = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.cabinetApi.list().subscribe({
      next: (cabinets) => this.cabinets.set(cabinets),
      error: (error: HttpErrorResponse) => this.notify('Could not load cabinets', this.errorMessage(error)),
    });
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

  showCabinets(): void {
    this.view.set('cabinets');
  }

  /** Opens the accounts tab, optionally limited to the members of one cabinet. */
  showAccounts(cabinetId: number | null = null): void {
    this.cabinetFilter.set(cabinetId);
    this.view.set('accounts');
  }

  openCabinetCreate(): void {
    this.selectedCabinet = null;
    this.cabinetForm = this.emptyCabinetForm();
    this.openDialog('cabinet-create');
  }

  openCabinetEdit(cabinet: Cabinet): void {
    this.selectedCabinet = cabinet;
    this.cabinetForm = {
      name: cabinet.name,
      code: cabinet.code,
      address: cabinet.address ?? '',
      phoneNumber: cabinet.phoneNumber ?? '',
      email: cabinet.email ?? '',
      active: cabinet.active,
      copyCatalogFromCabinetId: null,
      managerFullName: '',
      managerUsername: '',
    };
    this.openDialog('cabinet-edit');
  }

  toggleCabinetActive(cabinet: Cabinet): void {
    this.save(
      this.cabinetApi.update(cabinet.id, {
        name: cabinet.name,
        code: cabinet.code,
        address: cabinet.address,
        phoneNumber: cabinet.phoneNumber,
        email: cabinet.email,
        active: !cabinet.active,
      }),
      cabinet.active
        ? `${cabinet.name} was disabled. Its members can no longer sign in.`
        : `${cabinet.name} was enabled.`,
    );
  }

  deleteCabinet(cabinet: Cabinet): void {
    if (typeof window !== 'undefined' && !window.confirm(`Delete the empty cabinet "${cabinet.name}"?`)) return;
    this.save(this.cabinetApi.delete(cabinet.id), `${cabinet.name} was deleted.`);
  }

  contactLine(cabinet: Cabinet): string {
    return [cabinet.address, cabinet.phoneNumber, cabinet.email].filter(Boolean).join(' · ');
  }

  isCabinetEmpty(cabinet: Cabinet): boolean {
    const { doctors, secretaries, patients, appointments, treatments } = cabinet.stats;
    return doctors + secretaries + patients + appointments + treatments === 0;
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
    this.form = { ...this.emptyForm(), cabinetId: this.cabinetFilter() ?? this.activeCabinets()[0]?.id ?? null };
    this.openDialog('create');
  }

  openEdit(user: AdminUser): void {
    this.selected = user;
    this.form = {
      ...this.emptyForm(),
      username: user.username,
      fullName: user.fullName,
      role: user.role,
      active: user.active,
      cabinetId: user.cabinetId,
    };
    this.openDialog('edit');
  }

  /** A fresh one-time link: first password for a new account, or a new password for a forgotten one. */
  sendInvitation(user: AdminUser): void {
    this.saving.set(true);
    this.users.invite(user.id).subscribe({
      next: (invitation) => this.showInvitation(invitation),
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notify('Link not created', this.errorMessage(error));
      },
    });
  }

  openBranding(cabinet: Cabinet): void {
    this.selectedCabinet = cabinet;
    this.openDialog('branding');
  }

  closeBranding(): void {
    this.dialogMode = null;
    this.selectedCabinet = null;
    this.load();
  }

  unlock(user: AdminUser): void {
    this.save(this.users.unlock(user.id), `${user.fullName} can sign in again.`);
  }

  openReplaceManager(cabinet: Cabinet): void {
    this.selectedCabinet = cabinet;
    this.cabinetForm = { ...this.emptyCabinetForm(), name: cabinet.name };
    this.openDialog('manager');
  }

  closeInvitation(): void {
    this.invitation = null;
  }

  closeDialog(): void {
    if (this.saving()) return;
    this.dialogMode = null;
    this.selected = null;
    this.selectedCabinet = null;
  }

  get isCabinetDialog(): boolean {
    return this.dialogMode === 'cabinet-create' || this.dialogMode === 'cabinet-edit';
  }

  private showInvitation(invitation: Invitation): void {
    this.saving.set(false);
    this.dialogMode = null;
    this.selected = null;
    this.selectedCabinet = null;
    this.invitation = invitation;
    this.load();
  }

  get dialogTitle(): string {
    if (this.dialogMode === 'manager') return `Manager of ${this.selectedCabinet?.name ?? 'cabinet'}`;
    if (this.dialogMode === 'cabinet-create') return 'New cabinet';
    if (this.dialogMode === 'cabinet-edit') return `Edit ${this.selectedCabinet?.name ?? 'cabinet'}`;
    if (this.dialogMode === 'create') return 'New account';
    return `Edit ${this.selected?.fullName ?? 'account'}`;
  }

  toggleActive(user: AdminUser): void {
    this.save(
      this.users.update(user.id, {
        username: user.username,
        fullName: user.fullName,
        role: user.role,
        active: !user.active,
        cabinetId: user.cabinetId,
      }),
      user.active ? `${user.fullName} was deactivated.` : `${user.fullName} was reactivated.`,
    );
  }

  submit(): void {
    this.formError = this.validate();
    if (this.formError) return;

    if (this.dialogMode === 'manager' && this.selectedCabinet) {
      const { managerFullName, managerUsername } = this.cabinetForm;
      this.saving.set(true);
      this.cabinetApi
        .replaceManager(this.selectedCabinet.id, { fullName: managerFullName.trim(), username: managerUsername.trim() })
        .subscribe({
          next: (created) => this.showInvitation(created.invitation),
          error: (error: HttpErrorResponse) => this.fail(error),
        });
      return;
    }

    if (this.isCabinetDialog) {
      const { name, code, address, phoneNumber, email, active, copyCatalogFromCabinetId } = this.cabinetForm;
      const base = {
        name: name.trim(),
        code: code.trim(),
        address: address.trim() || null,
        phoneNumber: phoneNumber.trim() || null,
        email: email.trim() || null,
      };
      this.saving.set(true);
      if (this.dialogMode === 'cabinet-create') {
        this.cabinetApi
          .create({
            ...base,
            active,
            copyCatalogFromCabinetId,
            manager: {
              fullName: this.cabinetForm.managerFullName.trim(),
              username: this.cabinetForm.managerUsername.trim(),
            },
          })
          .subscribe({
            next: (created) => this.showInvitation(created.managerInvitation),
            error: (error: HttpErrorResponse) => this.fail(error),
          });
      } else if (this.selectedCabinet) {
        this.cabinetApi.update(this.selectedCabinet.id, { ...base, active }).subscribe(this.done(`${base.name} was updated.`));
      }
      return;
    }

    const { username, fullName, role, active } = this.form;
    const cabinetId = role === 'admin' ? null : this.form.cabinetId;
    this.saving.set(true);
    if (this.dialogMode === 'create') {
      this.users.create({ username, fullName, role, cabinetId }).subscribe({
        next: (created) => this.showInvitation(created.invitation),
        error: (error: HttpErrorResponse) => this.fail(error),
      });
    } else if (this.dialogMode === 'edit' && this.selected) {
      this.users
        .update(this.selected.id, { username, fullName, role, active, cabinetId })
        .subscribe(this.done(`${fullName} was updated.`));
    }
  }

  logout(): void {
    this.lifecycle.logout();
  }

  private save(request: Observable<unknown>, successMessage: string): void {
    this.saving.set(true);
    request.subscribe(this.done(successMessage));
  }

  private done(successMessage: string) {
    return {
      next: () => {
        this.saving.set(false);
        this.dialogMode = null;
        this.selected = null;
        this.selectedCabinet = null;
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
    if (this.dialogMode === 'manager' || this.dialogMode === 'cabinet-create') {
      const { managerFullName, managerUsername } = this.cabinetForm;
      if (!managerFullName.trim()) return 'The manager\'s full name is required.';
      if (!/^[a-zA-Z0-9._-]{3,80}$/.test(managerUsername.trim())) {
        return 'Manager username must be 3-80 characters: letters, digits, dot, dash or underscore.';
      }
    }
    if (this.dialogMode === 'manager') return '';
    if (this.isCabinetDialog) {
      const { name, code } = this.cabinetForm;
      if (!name.trim()) return 'Cabinet name is required.';
      if (!/^[a-zA-Z0-9_-]{2,40}$/.test(code.trim())) {
        return 'Cabinet code must be 2-40 characters: letters, digits, dash or underscore.';
      }
      return '';
    }
    const { username, fullName } = this.form;
    if (!/^[a-zA-Z0-9._-]{3,80}$/.test(username.trim())) {
      return 'Username must be 3-80 characters: letters, digits, dot, dash or underscore.';
    }
    if (!fullName.trim()) return 'Full name is required.';
    if (this.form.role !== 'admin' && this.form.cabinetId === null) {
      return 'Choose the cabinet this account belongs to.';
    }
    return '';
  }

  private openDialog(mode: DialogMode): void {
    this.formError = '';
    this.dialogMode = mode;
  }

  private emptyForm(): UserForm {
    return {
      username: '',
      fullName: '',
      role: 'secretaire',
      active: true,
      cabinetId: null,
    };
  }

  private emptyCabinetForm(): CabinetForm {
    return {
      name: '',
      code: '',
      address: '',
      phoneNumber: '',
      email: '',
      active: true,
      copyCatalogFromCabinetId: null,
      managerFullName: '',
      managerUsername: '',
    };
  }

  private notify(title: string, message: string): void {
    this.notifications.show({ kind: 'system', title, message });
  }

  private errorMessage(error: HttpErrorResponse): string {
    const messages = error.error?.messages;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.status === 403) return 'Only an admin can manage cabinets and accounts.';
    if (error.status === 0) return 'The server is unreachable.';
    return 'Something went wrong. Please try again.';
  }
}
