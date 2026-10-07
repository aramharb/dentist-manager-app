import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { clinicalNotes, doctorInstructions } from '../../secretaire/data/mock-secretary.data';
import { doctorClients, doctorTreatments, labelFr, lowDoctorMaterials, todaysDoctorAppointments, stockPercent } from '../doctor-dashboard.data';
import { SessionService } from '../../core/auth/session.service';

@Component({
  selector: 'app-doctor-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="dashboard-grid">
      <article class="panel">
        <div class="section-heading"><h2>Planning du jour</h2><span>{{ appointments.length }} visites</span></div>
        <div class="row-card" *ngFor="let appt of appointments" [style.--accent]="appt.color">
          <div class="avatar">{{ appt.photo }}</div>
          <div><strong>{{ appt.time }} - {{ appt.clientName }}</strong><p>{{ appt.treatmentType }} - {{ appt.duration }} min - {{ appt.room }}</p><small>{{ labelFr(appt.status) }} / {{ labelFr(appt.priority) }}</small></div>
        </div>
        <div class="action-row"><button type="button" routerLink="/doctor/schedule">Ouvrir le planning</button></div>
      </article>

      <article class="panel">
        <div class="owner-mini" *ngIf="currentUser$ | async as user"><div><strong>{{ user.fullName }}</strong><small>Doctor workspace</small></div></div>
        <div class="section-heading"><h2>Suivi du stock</h2><span>{{ lowMaterials.length }} alertes</span></div>
        <div class="row-card" *ngFor="let material of lowMaterials">
          <div>
            <strong>{{ material.name }}</strong>
            <p>{{ material.supplier }} - {{ labelFr(material.status) }}</p>
            <div class="progress"><i [style.width.%]="stockPercent(material)"></i></div>
          </div>
        </div>
        <div class="action-row"><button type="button" routerLink="/doctor/materials">Verifier le stock</button></div>
      </article>

      <article class="panel">
        <div class="section-heading"><h2>Suivi des patients</h2><span>{{ clients.length }} dossiers actifs</span></div>
        <div class="row-card" *ngFor="let client of clients">
          <span class="avatar">{{ client.avatar }}</span>
          <div><strong>{{ client.firstName }} {{ client.lastName }}</strong><p>{{ client.mainComplaint }}</p><small>{{ client.treatmentProgress }}% d'avancement du traitement</small></div>
        </div>
      </article>

      <article class="panel">
        <div class="section-heading"><h2>Revenus par soin</h2><span>Par traitement</span></div>
        <div class="bar-chart">
          <div *ngFor="let item of treatments" [style.--bar]="item.count + '%'">
            <span>{{ item.treatment }}</span><i [style.background]="item.color"></i><strong>{{ item.revenue | currency }}</strong>
          </div>
        </div>
        <div class="action-row"><button type="button" routerLink="/doctor/money">Ouvrir les finances</button></div>
      </article>
      <article class="panel">
        <div class="section-heading"><h2>Patients waiting</h2><span>Reception</span></div>
        <div class="row-card" *ngFor="let client of waitingPatients"><span class="avatar">{{ client.avatar }}</span><div><strong>{{ client.firstName }} {{ client.lastName }}</strong><p>Waiting for {{ client.mainComplaint }}</p><small>Notify doctor when room is ready</small></div></div>
      </article>
      <article class="panel">
        <div class="section-heading"><h2>Recent notes</h2><span>Doctor only</span></div>
        <div class="note-card" *ngFor="let note of notes"><strong>{{ note.patientName }}</strong><p>{{ note.diagnosis }}</p><small>{{ note.date }}</small></div>
      </article>
      <article class="panel wide-profile">
        <div class="section-heading"><h2>Latest treatments and secretary instructions</h2><span>Live handoff</span></div>
        <div class="row-card" *ngFor="let instruction of instructions"><div><strong>{{ instruction.patientName }}</strong><p>{{ instruction.text }}</p><small>{{ instruction.priority }} · {{ instruction.due }}</small></div><span class="pill">Secretary</span></div>
      </article>
    </section>
  `,
  styleUrl: './doctor-page.css',
})
export class DoctorOverviewComponent {
  readonly currentUser$ = inject(SessionService).currentUser$;
  appointments = todaysDoctorAppointments;
  clients = doctorClients;
  lowMaterials = lowDoctorMaterials;
  treatments = doctorTreatments;
  waitingPatients = doctorClients.slice(0, 2);
  notes = clinicalNotes;
  instructions = doctorInstructions.slice(0, 3);
  stockPercent = stockPercent;
  labelFr = labelFr;
}
