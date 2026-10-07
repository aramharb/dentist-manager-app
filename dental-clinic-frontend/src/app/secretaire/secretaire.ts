import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Subscription, merge } from 'rxjs';
import { SessionLifecycleService } from '../core/auth/session-lifecycle.service';
import { SessionService } from '../core/auth/session.service';
import { AppLanguageService } from '../core/i18n/app-language.service';
import { LanguageSwitcherComponent } from '../core/i18n/language-switcher.component';
import { MessageService } from '../shared/services/message.service';
import { appointments, clients, materials } from './data/mock-secretary.data';
import { ClinicIconComponent } from './shared/clinic-icon/clinic-icon.component';
import { NavItemComponent } from './shared/nav-item/nav-item.component';
import { AppointmentService } from './services/appointment.service';

@Component({
  selector: 'app-secretaire',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterOutlet, NavItemComponent, ClinicIconComponent, LanguageSwitcherComponent],
  templateUrl: './secretaire.html',
  styleUrl: './secretaire.css',
})
export class Secretaire implements OnInit, OnDestroy {
  private readonly session = inject(SessionService);
  private readonly sessionLifecycle = inject(SessionLifecycleService);
  private readonly messageService = inject(MessageService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly appointmentService = inject(AppointmentService);
  private readonly language = inject(AppLanguageService);

  secretary = { id: 0, fullName: '', role: 'Medical secretary', avatar: 'U' };
  notifications: { count: number; userId?: number; title: string; message: string; time: string; read: boolean }[] = [];
  mobileMenuOpen = false;
  notificationsOpen = false;
  private notificationRequest?: Subscription;
  searchOpen = false;
  searchQuery = '';
  currentDateTime = '';
  private readonly messageSubscriptions = new Subscription();
  private clockTimer?: ReturnType<typeof setInterval>;
  navItems = [
    { icon: 'home', label: 'Accueil', link: '/secretaire/home' },
    { icon: 'users', label: 'Clients', link: '/secretaire/clients' },
    { icon: 'calendar', label: 'Rendez-vous', link: '/secretaire/appointments' },
    { icon: 'briefcase', label: 'Payments', link: '/secretaire/payments' },
    { icon: 'chart', label: 'Expenses', link: '/secretaire/expenses' },
    { icon: 'bell', label: 'Messages', link: '/secretaire/messages' },
    { icon: 'inventory', label: 'Materials', link: '/secretaire/materials' },
    { icon: 'chart', label: 'Insights', link: '/secretaire/dashboard' },
  ];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.updateClock();
    this.clockTimer = setInterval(() => this.updateClock(), 60_000);
    this.messageSubscriptions.add(this.language.language$.subscribe(() => {
      this.updateClock();
      this.loadMessageNotifications();
    }));
    this.messageSubscriptions.add(this.session.currentUser$.subscribe((user) => {
      if (!user) return;
      this.secretary = {
        id: user.id,
        fullName: user.fullName,
        role: user.role === 'doctor' ? 'Doctor' : 'Medical secretary',
        avatar: this.initials(user.fullName),
      };
      this.cdr.markForCheck();
    }));
    this.loadMessageNotifications();
    this.messageSubscriptions.add(
      merge(this.messageService.messages$, this.messageService.notificationsChanged$).subscribe(() => this.loadMessageNotifications()),
    );
  }

  ngOnDestroy(): void {
    if (this.clockTimer) clearInterval(this.clockTimer);
    this.notificationRequest?.unsubscribe();
    this.messageSubscriptions.unsubscribe();
  }

  get unreadNotifications(): number {
    return this.notifications.reduce((count, notification) => count + notification.count, 0);
  }

  get searchResults() {
    const value = this.searchQuery.trim().toLowerCase();
    if (!value) return [];
    const clientResults = clients.filter((client) => `${client.firstName} ${client.lastName} ${client.contact.phone} ${client.assignedDentist}`.toLowerCase().includes(value)).slice(0, 4).map((client) => ({ label: `${client.firstName} ${client.lastName}`, detail: `${client.status} - ${client.assignedDentist}`, link: '/secretaire/clients', badge: 'Client' }));
    const appointmentResults = appointments.filter((appointment) => `${appointment.clientName} ${appointment.treatmentType} ${appointment.dentist}`.toLowerCase().includes(value)).slice(0, 4).map((appointment) => ({ label: appointment.clientName, detail: `${appointment.time} - ${appointment.treatmentType}`, link: '/secretaire/appointments', badge: 'Visit' }));
    const materialResults = materials.filter((material) => `${material.name} ${material.category} ${material.supplier}`.toLowerCase().includes(value)).slice(0, 4).map((material) => ({ label: material.name, detail: `${material.status} - ${material.quantity} ${material.unit}`, link: '/secretaire/materials', badge: 'Stock' }));
    return [...clientResults, ...appointmentResults, ...materialResults].slice(0, 8);
  }

  openSearch(): void {
    this.searchOpen = true;
    this.notificationsOpen = false;
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
    }
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

  logout(): void {
    this.sessionLifecycle.logout();
  }

  navigate(link: string): void {
    this.mobileMenuOpen = false;
    if (link.endsWith('/appointments')) this.appointmentService.requestRefresh();
  }
}
