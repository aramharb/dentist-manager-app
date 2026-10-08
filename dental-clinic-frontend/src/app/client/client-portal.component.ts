import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  CabinetBranding,
  ClientApiService,
  ClientSessionService,
  DoctorOption,
  JoinProfile,
  Membership,
  MyRequest,
  SlotDay,
  cabinetImageUrl,
} from './api/client-api.service';

type AuthMode = 'login' | 'register';
type Step = 'cabinet' | 'details' | 'doctor' | 'time' | 'done';

const STATUS_LABELS: Record<MyRequest['effectiveStatus'], string> = {
  PENDING: 'En attente de confirmation',
  CONFIRMED: 'Confirmé',
  REJECTED: 'Refusé',
  CANCELLED: 'Annulé',
  CANCELLED_BY_CABINET: 'Annulé par le cabinet',
};

/**
 * Client (patient) space: create an account with a phone number, choose a cabinet, send one's details
 * to it, then choose a doctor and a free time. The cabinet's secretary confirms the appointment.
 */
@Component({
  selector: 'app-client-portal',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './client-portal.component.html',
  styleUrl: './client-portal.component.css',
})
export class ClientPortalComponent implements OnInit {
  private readonly api = inject(ClientApiService);
  readonly session = inject(ClientSessionService);

  readonly cabinets = signal<CabinetBranding[]>([]);
  readonly memberships = signal<Membership[]>([]);
  readonly requests = signal<MyRequest[]>([]);
  readonly loadingCabinets = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');

  // sign in / sign up
  mode: AuthMode = 'login';
  phone = '';
  password = '';
  fullName = '';

  // booking
  readonly step = signal<Step>('cabinet');
  readonly selected = signal<CabinetBranding | null>(null);
  readonly doctors = signal<DoctorOption[]>([]);
  readonly selectedDoctor = signal<DoctorOption | null>(null);
  readonly days = signal<SlotDay[]>([]);
  readonly loadingTimes = signal(false);
  readonly chosen = signal<{ date: string; time: string } | null>(null);
  readonly booked = signal<MyRequest | null>(null);
  profile: JoinProfile = this.emptyProfile();
  profileLocked = false;
  note = '';
  weekStart = this.today();

  readonly accent = computed(() => this.selected()?.primaryColor ?? '#1689E8');
  readonly accentText = computed(() => this.contrastColor(this.accent()));
  readonly pendingCount = computed(() => this.requests().filter((r) => r.status === 'PENDING').length);

  ngOnInit(): void {
    this.api.cabinets().subscribe({
      next: (cabinets) => {
        this.cabinets.set(cabinets);
        this.loadingCabinets.set(false);
      },
      error: () => this.loadingCabinets.set(false),
    });
    if (this.session.isAuthenticated) this.loadAccountData();
  }

  // ------------------------------------------------------------------ account

  submitAuth(): void {
    this.error.set('');
    if (!this.phone.trim() || !this.password) return this.error.set('Entrez votre numéro de téléphone et votre mot de passe.');
    if (this.mode === 'register') {
      if (!this.fullName.trim()) return this.error.set('Entrez votre nom complet.');
      if (this.password.length < 8) return this.error.set('Le mot de passe doit contenir au moins 8 caractères.');
    }
    this.busy.set(true);
    const call =
      this.mode === 'register'
        ? this.api.register(this.fullName.trim(), this.phone.trim(), this.password)
        : this.api.login(this.phone.trim(), this.password);
    call.subscribe({
      next: () => {
        this.busy.set(false);
        this.password = '';
        this.loadAccountData();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  accountOpen = false;
  deletePassword = '';

  downloadMyData(): void {
    this.api.exportData().subscribe({
      next: (json) => {
        const url = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = 'mes-donnees.json';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  deleteMyAccount(): void {
    this.error.set('');
    if (!this.deletePassword) return this.error.set('Entrez votre mot de passe pour confirmer.');
    if (typeof window !== 'undefined' && !window.confirm('Supprimer définitivement votre compte et vos demandes ?')) return;
    this.busy.set(true);
    this.api.deleteAccount(this.deletePassword).subscribe({
      next: () => {
        this.busy.set(false);
        this.deletePassword = '';
        this.accountOpen = false;
        this.logout();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  logout(): void {
    this.session.clear();
    this.memberships.set([]);
    this.requests.set([]);
    this.resetBooking();
  }

  // ------------------------------------------------------------------ booking

  chooseCabinet(cabinet: CabinetBranding): void {
    this.error.set('');
    this.selected.set(cabinet);
    const membership = this.memberships().find((m) => m.cabinet.cabinetId === cabinet.cabinetId);
    if (membership && membership.status !== 'REJECTED') {
      this.profile = { ...membership.profile };
      this.profileLocked = membership.status === 'LINKED';
      this.loadDoctors();
    } else {
      const [first, ...rest] = (this.session.account()?.fullName ?? '').split(' ');
      this.profile = { ...this.emptyProfile(), firstName: first ?? '', lastName: rest.join(' ') };
      this.profileLocked = false;
      this.step.set('details');
    }
    this.scrollTop();
  }

  editDetails(): void {
    this.step.set('details');
  }

  saveDetails(): void {
    const cabinet = this.selected();
    if (!cabinet) return;
    this.error.set('');
    if (!this.profile.firstName.trim() || !this.profile.lastName.trim() || !this.profile.gender) {
      return this.error.set('Le prénom, le nom et le sexe sont obligatoires.');
    }
    this.busy.set(true);
    this.api.join(cabinet.cabinetId, this.cleanProfile()).subscribe({
      next: (membership) => {
        this.busy.set(false);
        this.memberships.update((items) => [
          membership,
          ...items.filter((m) => m.cabinet.cabinetId !== cabinet.cabinetId),
        ]);
        this.loadDoctors();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  chooseDoctor(doctor: DoctorOption): void {
    this.selectedDoctor.set(doctor);
    this.chosen.set(null);
    this.weekStart = this.today();
    this.step.set('time');
    this.loadTimes();
  }

  previousWeek(): void {
    if (!this.canGoBack) return;
    this.weekStart = this.shift(this.weekStart, -7) < this.today() ? this.today() : this.shift(this.weekStart, -7);
    this.loadTimes();
  }

  nextWeek(): void {
    if (!this.canGoForward) return;
    this.weekStart = this.shift(this.weekStart, 7);
    this.loadTimes();
  }

  get canGoBack(): boolean {
    return this.weekStart > this.today();
  }

  get canGoForward(): boolean {
    return this.shift(this.weekStart, 7) <= this.shift(this.today(), 60);
  }

  pick(day: SlotDay, time: string): void {
    this.chosen.set({ date: day.date, time });
  }

  submitRequest(): void {
    const cabinet = this.selected();
    const doctor = this.selectedDoctor();
    const slot = this.chosen();
    if (!cabinet || !doctor || !slot) return;
    this.error.set('');
    this.busy.set(true);
    this.api.request(cabinet.cabinetId, doctor.id, slot.date, slot.time, this.note).subscribe({
      next: (request) => {
        this.busy.set(false);
        this.booked.set(request);
        this.step.set('done');
        this.loadAccountData();
        this.scrollTop();
      },
      error: (error: HttpErrorResponse) => {
        this.fail(error);
        if (error.status === 409) this.loadTimes();
      },
    });
  }

  cancel(request: MyRequest): void {
    if (typeof window !== 'undefined' && !window.confirm('Annuler cette demande de rendez-vous ?')) return;
    this.api.cancel(request.id).subscribe({
      next: () => this.loadAccountData(),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  backToCabinets(): void {
    this.resetBooking();
  }

  backToDoctors(): void {
    this.step.set('doctor');
    this.chosen.set(null);
  }

  // ------------------------------------------------------------------ view helpers

  imageUrl(cabinet: CabinetBranding, kind: 'logo' | 'cover'): string {
    return cabinetImageUrl(cabinet, kind);
  }

  initials(text: string): string {
    return text
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  statusLabel(request: MyRequest): string {
    return STATUS_LABELS[request.effectiveStatus] ?? request.effectiveStatus;
  }

  membershipOf(cabinet: CabinetBranding): Membership | undefined {
    return this.memberships().find((m) => m.cabinet.cabinetId === cabinet.cabinetId);
  }

  formatDay(iso: string): string {
    return new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  formatTime(time: string): string {
    return time.slice(0, 5);
  }

  // ------------------------------------------------------------------ internals

  private loadAccountData(): void {
    this.api.memberships().subscribe({ next: (items) => this.memberships.set(items), error: (e) => this.fail(e) });
    this.api.requests().subscribe({ next: (items) => this.requests.set(items), error: (e) => this.fail(e) });
  }

  private loadDoctors(): void {
    const cabinet = this.selected();
    if (!cabinet) return;
    this.busy.set(true);
    this.api.doctors(cabinet.cabinetId).subscribe({
      next: (doctors) => {
        this.busy.set(false);
        this.doctors.set(doctors);
        this.step.set('doctor');
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private loadTimes(): void {
    const cabinet = this.selected();
    const doctor = this.selectedDoctor();
    if (!cabinet || !doctor) return;
    this.loadingTimes.set(true);
    this.api.freeTimes(cabinet.cabinetId, doctor.id, this.weekStart, 7).subscribe({
      next: (days) => {
        this.loadingTimes.set(false);
        this.days.set(days);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingTimes.set(false);
        this.fail(error);
      },
    });
  }

  private resetBooking(): void {
    this.step.set('cabinet');
    this.selected.set(null);
    this.selectedDoctor.set(null);
    this.chosen.set(null);
    this.booked.set(null);
    this.days.set([]);
    this.note = '';
    this.error.set('');
  }

  private cleanProfile(): JoinProfile {
    const blank = (value: string | null) => (value && value.trim() ? value.trim() : null);
    return {
      ...this.profile,
      firstName: this.profile.firstName.trim(),
      lastName: this.profile.lastName.trim(),
      birthDate: this.profile.birthDate || null,
      address: blank(this.profile.address),
      email: blank(this.profile.email),
      bloodType: this.profile.bloodType || null,
      allergies: blank(this.profile.allergies),
      cnamNumber: this.profile.cnamCovered ? blank(this.profile.cnamNumber) : null,
    };
  }

  private emptyProfile(): JoinProfile {
    return {
      firstName: '',
      lastName: '',
      gender: 'Male',
      birthDate: null,
      address: null,
      email: null,
      bloodType: null,
      allergies: null,
      cnamCovered: false,
      cnamNumber: null,
    };
  }

  private fail(error: HttpErrorResponse): void {
    this.busy.set(false);
    const messages = error.error?.messages;
    if (Array.isArray(messages) && messages.length) this.error.set(messages.join(' '));
    else if (error.status === 401) this.error.set('Session expirée. Reconnectez-vous.');
    else if (error.status === 0) this.error.set('Le serveur est injoignable.');
    else this.error.set('Une erreur est survenue. Réessayez.');
  }

  private today(): string {
    return this.toIso(new Date());
  }

  private shift(iso: string, days: number): string {
    const date = new Date(`${iso}T00:00:00`);
    date.setDate(date.getDate() + days);
    return this.toIso(date);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  /** Black or white text, whichever reads better on the cabinet colour. */
  private contrastColor(hex: string): string {
    const value = hex.replace('#', '');
    const [r, g, b] = [0, 2, 4].map((i) => parseInt(value.slice(i, i + 2), 16));
    return (r * 299 + g * 587 + b * 114) / 1000 > 150 ? '#10202e' : '#ffffff';
  }

  private scrollTop(): void {
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
