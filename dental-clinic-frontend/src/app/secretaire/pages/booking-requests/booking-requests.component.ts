import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../core/notifications/notification.service';
import { PatientService } from '../../services/patient.service';
import { Patient } from '../../models/secretary.models';

interface PatientMatch {
  id: number;
  patientNumber: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  birthDate: string | null;
}

interface PendingRequest {
  id: number;
  clientName: string;
  clientPhone: string;
  doctorId: number;
  doctorName: string;
  date: string;
  startTime: string;
  endTime: string;
  note: string | null;
  createdAt: string;
  membershipStatus: 'PENDING' | 'LINKED' | 'REJECTED';
  linkedPatientId: number | null;
  profile: {
    firstName: string;
    lastName: string;
    gender: string;
    birthDate: string | null;
    address: string | null;
    email: string | null;
    bloodType: string | null;
    allergies: string | null;
    cnamCovered: boolean;
    cnamNumber: string | null;
  };
  matches: PatientMatch[];
}

/** Appointment requests sent from the client space: the secretary links the client to a file and confirms or refuses. */
@Component({
  selector: 'app-booking-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="panel">
      <header class="head">
        <div>
          <p class="eyebrow">Espace client</p>
          <h2>Demandes de rendez-vous en ligne</h2>
          <p class="muted">Confirmez ou refusez les demandes envoyées par les clients depuis leur espace.</p>
        </div>
        <button class="ghost" type="button" (click)="load()">Actualiser</button>
      </header>

      <p class="muted" *ngIf="loading()">Chargement…</p>
      <p class="muted" *ngIf="!loading() && !requests().length">Aucune demande en attente.</p>

      <article class="request" *ngFor="let request of requests()">
        <div class="when">
          <strong>{{ request.date | date: 'EEE d MMM' }} · {{ request.startTime.slice(0, 5) }}</strong>
          <span>avec {{ request.doctorName }}</span>
          <small *ngIf="request.note">« {{ request.note }} »</small>
        </div>

        <div class="who">
          <strong>{{ request.clientName }}</strong>
          <span>{{ request.clientPhone }}</span>
          <small>
            {{ request.profile.firstName }} {{ request.profile.lastName }} · {{ request.profile.gender === 'Female' ? 'Femme' : 'Homme' }}
            <ng-container *ngIf="request.profile.birthDate"> · né(e) le {{ request.profile.birthDate | date: 'dd/MM/yyyy' }}</ng-container>
            <ng-container *ngIf="request.profile.cnamCovered"> · CNAM {{ request.profile.cnamNumber }}</ng-container>
          </small>
          <small *ngIf="request.profile.allergies">Allergies : {{ request.profile.allergies }}</small>
        </div>

        <div class="link">
          <ng-container *ngIf="request.linkedPatientId; else chooseFile">
            <span class="tag ok">Dossier déjà lié (n° {{ request.linkedPatientId }})</span>
          </ng-container>
          <ng-template #chooseFile>
            <label>
              Dossier client
              <select [ngModel]="choice[request.id] ?? defaultChoice(request)" (ngModelChange)="choice[request.id] = $event" [name]="'choice' + request.id">
                <option value="new">Créer un nouveau dossier (assigné à {{ request.doctorName }})</option>
                <optgroup label="Même numéro de téléphone" *ngIf="request.matches.length">
                  <option *ngFor="let match of request.matches" [value]="'' + match.id">{{ match.lastName }} {{ match.firstName }} · {{ match.patientNumber }}</option>
                </optgroup>
                <optgroup label="Autre dossier existant">
                  <option *ngFor="let patient of patients()" [value]="'' + patient.id">{{ patient.lastName }} {{ patient.firstName }} · {{ patient.patientNumber }}</option>
                </optgroup>
              </select>
            </label>
            <span class="tag warn" *ngIf="request.matches.length">Un dossier avec le même numéro existe : vérifiez que c'est la même personne.</span>
          </ng-template>
        </div>

        <div class="actions">
          <button class="primary" type="button" [disabled]="busyId() === request.id" (click)="confirm(request)">Confirmer</button>
          <button class="ghost danger" type="button" [disabled]="busyId() === request.id" (click)="reject(request)">Refuser</button>
        </div>
      </article>
    </section>
  `,
  styles: [
    `
      .panel { display: grid; gap: 16px; }
      .head { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 12px; }
      h2 { margin: 0; }
      .eyebrow { margin: 0; color: var(--primary); font-size: 12px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; }
      .muted, small { color: var(--muted); }
      .request { display: grid; grid-template-columns: 1.1fr 1.4fr 1.6fr auto; gap: 18px; align-items: start; padding: 18px; border: 1px solid var(--line); border-radius: 18px; background: var(--surface-solid, #fff); }
      .when, .who, .link { display: grid; gap: 4px; }
      .actions { display: grid; gap: 8px; }
      label { display: grid; gap: 6px; font-size: 13px; font-weight: 600; }
      select { padding: 10px 12px; border: 1px solid var(--line); border-radius: 12px; font: inherit; background: #fff; }
      button { font: inherit; font-weight: 700; cursor: pointer; border-radius: 12px; padding: 10px 18px; }
      button:disabled { opacity: 0.55; cursor: default; }
      .primary { border: 0; color: #fff; background: linear-gradient(135deg, var(--primary), var(--cyan)); }
      .ghost { border: 1px solid var(--line); background: #fff; color: inherit; }
      .danger { color: var(--danger); }
      .tag { padding: 4px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
      .tag.ok { color: #0b7a56; background: rgba(25, 185, 133, 0.15); }
      .tag.warn { color: #92600a; background: rgba(245, 158, 11, 0.18); }
      @media (max-width: 1000px) { .request { grid-template-columns: 1fr; } .actions { grid-auto-flow: column; } }
    `,
  ],
})
export class BookingRequestsComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly patientService = inject(PatientService);
  private readonly notifications = inject(NotificationService);
  private readonly platformId = inject(PLATFORM_ID);
  private timer?: ReturnType<typeof setInterval>;

  readonly requests = signal<PendingRequest[]>([]);
  readonly patients = signal<Patient[]>([]);
  readonly loading = signal(true);
  readonly busyId = signal<number | null>(null);
  choice: Record<number, string | undefined> = {};

  ngOnInit(): void {
    this.load();
    this.patientService.getPatients().subscribe({ next: (patients) => this.patients.set(patients) });
    if (isPlatformBrowser(this.platformId)) this.timer = setInterval(() => this.load(), 30_000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  load(): void {
    this.http.get<PendingRequest[]>('/api/booking-requests').subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** A single matching file is suggested; otherwise a new file. */
  defaultChoice(request: PendingRequest): string {
    return request.matches.length === 1 ? String(request.matches[0].id) : 'new';
  }

  confirm(request: PendingRequest): void {
    const selected = this.choice[request.id] ?? this.defaultChoice(request);
    const patientId = request.linkedPatientId || selected === 'new' ? null : Number(selected);
    this.busyId.set(request.id);
    this.http.post(`/api/booking-requests/${request.id}/confirm`, { patientId }).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notify('Rendez-vous confirmé', `${request.clientName} · ${request.date} ${request.startTime.slice(0, 5)}`);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.busyId.set(null);
        this.notify('Confirmation impossible', error.error?.messages?.join?.(' ') ?? 'Réessayez.');
        this.load();
      },
    });
  }

  reject(request: PendingRequest): void {
    const reason = typeof window === 'undefined' ? '' : window.prompt('Motif du refus (facultatif) :', '') ?? null;
    if (reason === null) return;
    this.busyId.set(request.id);
    this.http.post(`/api/booking-requests/${request.id}/reject`, { reason: reason.trim() || null }).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notify('Demande refusée', request.clientName);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.busyId.set(null);
        this.notify('Refus impossible', error.error?.messages?.join?.(' ') ?? 'Réessayez.');
        this.load();
      },
    });
  }

  private notify(title: string, message: string): void {
    this.notifications.show({ kind: 'appointment', title, message });
  }
}
