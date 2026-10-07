import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, HostListener, OnDestroy, OnInit, PLATFORM_ID, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Subscription, merge } from 'rxjs';
import { SessionLifecycleService } from '../core/auth/session-lifecycle.service';
import { SessionService } from '../core/auth/session.service';
import { AppLanguageService } from '../core/i18n/app-language.service';
import { LanguageSwitcherComponent } from '../core/i18n/language-switcher.component';
import { ExpenseService } from '../shared/services/expense.service';
import { DashboardData, DashboardService } from '../shared/services/dashboard.service';
import { FinancialInsight } from '../shared/models/expense.models';
import { InventoryMaterial } from '../shared/models/material.models';
import { MaterialService } from '../shared/services/material.service';
import { MessageService, StaffAction } from '../shared/services/message.service';
import { StaffActionService } from '../shared/services/staff-action.service';
import { Appointment } from '../secretaire/models/secretary.models';
import { ClinicIconComponent } from '../secretaire/shared/clinic-icon/clinic-icon.component';
import { NavItemComponent } from '../secretaire/shared/nav-item/nav-item.component';
import { AppointmentService } from '../secretaire/services/appointment.service';
import { TreatmentPatientSummary } from './models/treatment.models';
import { TreatmentService } from './services/treatment.service';

@Component({
  selector: 'app-doctor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterOutlet, NavItemComponent, ClinicIconComponent, LanguageSwitcherComponent],
  templateUrl: './doctor.html',
  styleUrl: './doctor.css',
})
export class Doctor implements OnInit, OnDestroy {
  private readonly expenseService = inject(ExpenseService);
  private readonly dashboardService = inject(DashboardService);
  private readonly session = inject(SessionService);
  private readonly sessionLifecycle = inject(SessionLifecycleService);
  private readonly messageService = inject(MessageService);
  private readonly staffActionService = inject(StaffActionService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly appointmentService = inject(AppointmentService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly materialService = inject(MaterialService);
  private readonly language = inject(AppLanguageService);

  @ViewChild('globalSearch') globalSearch?: ElementRef<HTMLInputElement>;
  dashboard?: DashboardData;
  financialInsight?: FinancialInsight;
  notifications: { count: number; userId?: number; title: string; message: string; time: string; read: boolean }[] = [];
  staffActions: StaffAction[] = [];
  undoingActionId?: number;
  actionError = '';
  mobileMenuOpen = false;
  notificationsOpen = false;
  private notificationRequest?: Subscription;
  searchOpen = false;
  searchQuery = '';
  currentDateTime = '';
  darkMode = false;
  doctor = { fullName: '', role: 'Doctor', avatar: 'U' };
  searchPatients: TreatmentPatientSummary[] = [];
  searchAppointments: Appointment[] = [];
  searchMaterials: InventoryMaterial[] = [];
  private readonly realtimeSubscriptions = new Subscription();
  private clockTimer?: ReturnType<typeof setInterval>;
  private searchDataUserId?: number;
  navItems = [
    { icon: 'home', label: 'Home', link: '/doctor/dashboard' },
    { icon: 'users', label: 'Clients', link: '/doctor/clients' },
    { icon: 'calendar', label: 'Rendez-vous', link: '/doctor/appointments' },
    { icon: 'sparkles', label: 'Traitements', link: '/doctor/treatments' },
    { icon: 'briefcase', label: 'Payments', link: '/doctor/payments' },
    { icon: 'chart', label: 'Expenses', link: '/doctor/expenses' },
    { icon: 'bell', label: 'Messages', link: '/doctor/messages' },
    { icon: 'inventory', label: 'Materials', link: '/doctor/materials' },
  ];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.darkMode = localStorage.getItem('doctor-theme') === 'dark';
    this.updateClock();
    this.clockTimer = setInterval(() => this.updateClock(), 60_000);
    this.realtimeSubscriptions.add(this.language.language$.subscribe(() => {
      this.updateClock();
      this.loadMessageNotifications();
    }));
    this.loadHeaderData();
    this.realtimeSubscriptions.add(this.session.currentUser$.subscribe((user) => {
      if (!user) return;
      this.doctor = { fullName: user.fullName, role: this.roleLabel(user.role), avatar: this.initials(user.fullName) };
      this.loadSearchData(user.id);
      this.cdr.markForCheck();
    }));
    this.loadMessageNotifications();
    this.loadStaffActions();
    this.realtimeSubscriptions.add(
      merge(this.messageService.messages$, this.messageService.notificationsChanged$).subscribe(() => this.loadMessageNotifications()),
    );
    this.realtimeSubscriptions.add(
      this.messageService.staffActions$.subscribe((action) => {
        this.staffActions = [action, ...this.staffActions.filter((item) => item.id !== action.id)].slice(0, 100);
        this.cdr.markForCheck();
      }),
    );
  }

  ngOnDestroy(): void {
    if (this.clockTimer) clearInterval(this.clockTimer);
    this.notificationRequest?.unsubscribe();
    this.realtimeSubscriptions.unsubscribe();
  }

  get unreadNotifications(): number {
    return this.notifications.reduce((count, notification) => count + notification.count, 0);
  }

  get stats(): { label: string; value: string; hint: string }[] {
    const data = this.dashboard;
    return [
      { label: "Today's visits", value: String(data?.todayAppointments ?? 0), hint: 'Assigned appointments' },
      { label: 'Next 7 days', value: String(data?.upcomingAppointments ?? 0), hint: 'Upcoming assigned visits' },
      { label: 'Active treatments', value: String(data?.treatmentsInProgress ?? 0), hint: 'Your clinical workload' },
      { label: 'Assigned clients', value: String(data?.totalPatients ?? 0), hint: 'Patients under your care' },
      { label: 'Unread messages', value: String(this.unreadNotifications), hint: 'Personal inbox' },
      { label: 'Pending expenses', value: this.money(this.financialInsight?.pendingExpenses ?? 0), hint: 'Clinic bills awaiting payment' },
    ];
  }

  get searchResults() {
    const value = this.searchQuery.trim().toLowerCase();
    if (!value) return [];
    const clientResults = this.searchPatients
      .filter((client) => `${client.firstName} ${client.lastName} ${client.patientNumber} ${client.phoneNumber}`.toLowerCase().includes(value))
      .slice(0, 4)
      .map((client) => ({
        label: `${client.firstName} ${client.lastName}`,
        detail: `${client.patientNumber} · ${client.currentTreatment || 'No active treatment'}`,
        link: '/doctor/treatments',
        badge: 'Client',
      }));
    const appointmentResults = this.searchAppointments
      .filter((appointment) => `${appointment.clientName} ${appointment.treatmentType} ${appointment.date} ${appointment.time}`.toLowerCase().includes(value))
      .slice(0, 4)
      .map((appointment) => ({
        label: appointment.clientName,
        detail: `${appointment.date} ${appointment.time} · ${appointment.treatmentType}`,
        link: '/doctor/appointments',
        badge: 'Visit',
      }));
    const materialResults = this.searchMaterials
      .filter((material) => `${material.name} ${material.category} ${material.supplier || ''}`.toLowerCase().includes(value))
      .slice(0, 4)
      .map((material) => ({ label: material.name, detail: `${material.status} · ${material.quantity} ${material.unit}`, link: '/doctor/materials', badge: 'Stock' }));
    return [...clientResults, ...appointmentResults, ...materialResults].slice(0, 8);
  }

  openSearch(): void {
    this.searchOpen = true;
    this.notificationsOpen = false;
  }

  @HostListener('document:keydown', ['$event'])
  keyboardShortcuts(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.openSearch();
      setTimeout(() => this.globalSearch?.nativeElement.focus());
    } else if (event.key === 'Escape') {
      this.searchOpen = false;
      this.notificationsOpen = false;
    }
  }

  toggleTheme(): void {
    this.darkMode = !this.darkMode;
    localStorage.setItem('doctor-theme', this.darkMode ? 'dark' : 'light');
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchOpen = false;
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.searchOpen = false;
      this.loadMessageNotifications();
      this.loadStaffActions();
    }
  }

  undoAction(action: StaffAction): void {
    if (action.status === 'UNDONE' || !action.undoable || this.undoingActionId) return;
    this.undoingActionId = action.id;
    this.actionError = '';
    this.staffActionService.undo(action.id).subscribe({
      next: (updated) => {
        this.staffActions = this.staffActions.map((item) => item.id === updated.id ? updated : item);
        this.undoingActionId = undefined;
        this.cdr.markForCheck();
        this.loadHeaderData();
      },
      error: (error) => {
        const messages = error?.error?.messages;
        this.actionError = Array.isArray(messages) && messages.length
          ? messages.join(' ')
          : 'Unable to undo this action.';
        this.undoingActionId = undefined;
        this.cdr.markForCheck();
      },
    });
  }

  actionLabel(action: StaffAction): string {
    return action.actionType.toLowerCase().replaceAll('_', ' ');
  }

  private loadHeaderData(): void {
    this.dashboardService.getMine().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.cdr.markForCheck();
      },
      error: () => {
        this.dashboard = undefined;
        this.cdr.markForCheck();
      },
    });
    this.expenseService.getDoctorFinancialInsights().subscribe({
      next: (insight) => {
        this.financialInsight = insight;
        this.cdr.markForCheck();
      },
      error: () => {
        this.financialInsight = undefined;
        this.cdr.markForCheck();
      },
    });
  }

  private loadSearchData(userId: number): void {
    if (this.searchDataUserId === userId) return;
    this.searchDataUserId = userId;

    this.treatmentService.getMyPatients().subscribe({
      next: (patients) => {
        this.searchPatients = patients;
        this.cdr.markForCheck();
      },
      error: () => {
        this.searchPatients = [];
        this.cdr.markForCheck();
      },
    });
    this.appointmentService.getAppointments('', '', undefined, userId).subscribe({
      next: (appointments) => {
        this.searchAppointments = appointments;
        this.cdr.markForCheck();
      },
      error: () => {
        this.searchAppointments = [];
        this.cdr.markForCheck();
      },
    });
    this.materialService.getMaterials().subscribe({
      next: (materials) => {
        this.searchMaterials = materials;
        this.cdr.markForCheck();
      },
      error: () => {
        this.searchMaterials = [];
        this.cdr.markForCheck();
      },
    });
  }

  private updateClock(): void {
    this.currentDateTime = new Intl.DateTimeFormat(this.language.locale, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date());
    this.cdr.markForCheck();
  }

  private money(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }

  private loadStaffActions(): void {
    this.staffActionService.getRecent().subscribe({
      next: (actions) => { this.staffActions = actions; this.cdr.markForCheck(); },
      error: () => (this.staffActions = []),
    });
  }

  private loadMessageNotifications(): void {
    const userId = this.session.currentUser?.id;
    if (!userId) return;
    this.notificationRequest?.unsubscribe();
    this.notificationRequest = this.messageService.getConversations().subscribe({
      next: (conversations) => {
        this.notifications = conversations
          .filter((conversation) => conversation.unreadCount > 0)
          .map((conversation) => ({
            count: conversation.unreadCount,
            userId: conversation.participants.find(user => user.id !== userId)?.id,
            title: conversation.title,
            message: conversation.lastMessage?.body || `${conversation.unreadCount} unread messages`,
            time: conversation.lastMessage?.sentAt ? new Date(conversation.lastMessage.sentAt).toLocaleString(this.language.locale) : '',
            read: false,
          }));
        this.cdr.markForCheck();
      },
      error: () => { this.cdr.markForCheck(); },
    });
  }

  private initials(value: string): string {
    return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join('') || 'U';
  }

  logout(): void {
    this.sessionLifecycle.logout();
  }

  navigate(link: string): void {
    this.mobileMenuOpen = false;
    if (link.endsWith('/appointments')) this.appointmentService.requestRefresh();
  }

  private roleLabel(role: string): string {
    return role === 'doctor' ? 'Doctor' : 'Secretary';
  }
}
