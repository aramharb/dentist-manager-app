import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { doctorAppointments, labelFr } from '../doctor-dashboard.data';
import { SessionService } from '../../core/auth/session.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-doctor-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="dashboard-grid">
      <article class="panel">
        <div class="section-heading"><h2>Planning</h2><span>{{ filteredAppointments.length }} visites</span></div>
        <select [(ngModel)]="selectedDentist">
          <option>Tous les dentistes</option>
          <option *ngIf="currentUser$ | async as user">{{ user.fullName }}</option>
          <option>Dr. Lina Merabet</option>
        </select>
        <div class="row-card" *ngFor="let appt of filteredAppointments" [style.--accent]="appt.color">
          <span class="avatar">{{ appt.photo }}</span>
          <div>
            <strong>{{ appt.date }} - {{ appt.time }} - {{ appt.clientName }}</strong>
            <p>{{ appt.treatmentType }} dans {{ appt.room }} avec {{ appt.dentist }}</p>
            <small>{{ appt.duration }} min - {{ labelFr(appt.status) }} - {{ labelFr(appt.priority) }}</small>
          </div>
          <span class="pill">{{ labelFr(appt.status) }}</span>
        </div>
      </article>
      <article class="panel">
        <div class="owner-mini" *ngIf="currentUser$ | async as user"><div><strong>{{ user.fullName }}</strong><small>Doctor workspace</small></div></div>
        <div class="section-heading"><h2>Actions du planning</h2><span>Outils medecin</span></div>
        <div class="row-card"><div><strong>{{ urgentCount }} cas urgents</strong><p>Visites prioritaires a reviser.</p></div></div>
        <div class="row-card"><div><strong>{{ totalMinutes }} minutes reservees</strong><p>Temps total des soins visibles.</p></div></div>
        <div class="action-row"><button type="button">Demarrer la visite</button><button type="button" class="ghost-button">Reporter</button><button type="button" class="ghost-button">Imprimer la journee</button></div>
      </article>
    </section>
  `,
  styleUrl: './doctor-page.css',
})
export class DoctorScheduleComponent {
  readonly currentUser$ = inject(SessionService).currentUser$;
  appointments = doctorAppointments;
  selectedDentist = 'Tous les dentistes';
  labelFr = labelFr;

  get filteredAppointments() {
    if (this.selectedDentist === 'Tous les dentistes') return this.appointments;
    return this.appointments.filter((appointment) => appointment.dentist === this.selectedDentist);
  }

  get urgentCount(): number {
    return this.appointments.filter((appointment) => appointment.priority !== 'Routine').length;
  }

  get totalMinutes(): number {
    return this.filteredAppointments.reduce((total, appointment) => total + appointment.duration, 0);
  }
}
