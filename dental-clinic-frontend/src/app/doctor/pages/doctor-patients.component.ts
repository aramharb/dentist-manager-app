import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { clinicalNotes, doctorInstructions, patientPayments, treatmentCosts, treatmentSteps } from '../../secretaire/data/mock-secretary.data';
import { doctorClients, labelFr, patientBalance, patientLifetimeValue } from '../doctor-dashboard.data';
import { SessionService } from '../../core/auth/session.service';

@Component({
  selector: 'app-doctor-patients',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="dashboard-grid">
      <article class="panel">
        <div class="section-heading"><h2>Dossiers patients</h2><span>{{ filteredClients.length }} affiches</span></div>
        <input class="search-input" [(ngModel)]="search" placeholder="Rechercher patient, dentiste, motif, telephone" />
        <div class="row-card" *ngFor="let client of filteredClients">
          <span class="avatar">{{ client.avatar }}</span>
          <div>
            <strong>{{ client.firstName }} {{ client.lastName }}</strong>
            <p>{{ client.mainComplaint }} - {{ client.assignedDentist }}</p>
            <small>{{ client.contact.phone }} - {{ labelFr(client.status) }} - Solde {{ client.billingBalance | currency }}</small>
          </div>
          <span class="pill">{{ client.treatmentProgress }}%</span>
        </div>
      </article>
      <article class="panel">
        <div class="owner-mini" *ngIf="currentUser$ | async as user"><div><strong>{{ user.fullName }}</strong><small>Doctor workspace</small></div></div>
        <div class="section-heading"><h2>Statistiques patients</h2><span>Frontend seulement</span></div>
        <div class="row-card"><div><strong>{{ clients.length }} patients au total</strong><p>Suivis dans les donnees de test.</p></div></div>
        <div class="row-card"><div><strong>{{ balance | currency }} solde ouvert</strong><p>Montant patient non regle.</p></div></div>
        <div class="row-card"><div><strong>{{ lifetime | currency }} valeur totale</strong><p>Valeur cumulee enregistree.</p></div></div>
        <div class="action-row"><button type="button">Ajouter une note</button><button type="button" class="ghost-button">Exporter la liste</button></div>
      </article>
      <article class="panel wide-profile">
        <div class="section-heading"><h2>Shared patient profile</h2><span>{{ selectedClient.firstName }} {{ selectedClient.lastName }}</span></div>
        <div class="tab-strip"><button *ngFor="let tab of tabs" [class.active]="activeTab === tab" (click)="activeTab = tab">{{ tab }}</button></div>
        <div class="profile-grid">
          <div class="row-card"><span class="avatar">{{ selectedClient.avatar }}</span><div><strong>{{ selectedClient.firstName }} {{ selectedClient.lastName }}</strong><p>{{ selectedClient.contact.phone }} · {{ selectedClient.contact.email }}</p><small>{{ selectedClient.medicalHistory }} · Allergies: {{ selectedClient.allergies }}</small></div></div>
          <div class="cost-card"><span>Treatment cost</span><strong>{{ selectedCost.finalBalance | currency }}</strong><small>Total {{ selectedCost.totalPrice | currency }} · {{ selectedCost.sessions }} sessions · insurance {{ selectedCost.insuranceCoverage | currency }}</small></div>
        </div>
        <div class="timeline">
          <div class="timeline-step" *ngFor="let step of selectedSteps" [class.completed]="step.status === 'Completed'">
            <strong>{{ step.label }}</strong><span>{{ step.status === 'Completed' ? '✔ Completed' : step.status }}</span><small>{{ step.date }} · {{ step.notes }}</small>
          </div>
        </div>
        <div class="action-row"><button type="button">Add treatment step</button><button class="ghost-button" type="button">Edit step</button><button class="ghost-button" type="button">Delete</button><button class="ghost-button" type="button">Reorder</button></div>
      </article>
      <article class="panel">
        <div class="section-heading"><h2>Clinical notes</h2><span>Doctor only</span></div>
        <label>Diagnosis<textarea rows="3" [(ngModel)]="note.diagnosis"></textarea></label>
        <label>Clinical observations<textarea rows="3" [(ngModel)]="note.observations"></textarea></label>
        <label>Prescription<textarea rows="3" [(ngModel)]="note.prescription"></textarea></label>
        <label>Recommendations<textarea rows="3" [(ngModel)]="note.recommendations"></textarea></label>
        <label>Future treatment plan<textarea rows="3" [(ngModel)]="note.futurePlan"></textarea></label>
      </article>
      <article class="panel">
        <div class="section-heading"><h2>Secretary instructions</h2><span>Auto-visible</span></div>
        <div class="note-card" *ngFor="let instruction of instructions"><strong>{{ instruction.patientName }}</strong><p>{{ instruction.text }}</p><span class="pill">{{ instruction.priority }}</span><small>{{ instruction.due }}</small></div>
        <label>New instruction<textarea rows="4">Ask patient to confirm next appointment and collect remaining balance.</textarea></label>
      </article>
    </section>
  `,
  styleUrl: './doctor-page.css',
})
export class DoctorPatientsComponent {
  readonly currentUser$ = inject(SessionService).currentUser$;
  clients = doctorClients;
  search = '';
  balance = patientBalance;
  lifetime = patientLifetimeValue;
  labelFr = labelFr;
  activeTab = 'Personal Information';
  tabs = ['Personal Information', 'Medical History', 'Allergies', 'Appointments', 'Payments', 'Treatment Plan', 'Clinical Notes', 'Documents', 'X-Rays', 'Photos', 'Messages', 'Timeline', 'Notifications'];
  instructions = doctorInstructions;
  note = clinicalNotes[0];

  get filteredClients() {
    const value = this.search.trim().toLowerCase();
    if (!value) return this.clients;
    return this.clients.filter((client) => `${client.firstName} ${client.lastName} ${client.assignedDentist} ${client.mainComplaint} ${client.contact.phone}`.toLowerCase().includes(value));
  }

  get selectedClient() {
    return this.filteredClients[0] ?? this.clients[0];
  }

  get selectedCost() {
    return treatmentCosts.find((cost) => cost.patientId === this.selectedClient.id) ?? treatmentCosts[0];
  }

  get selectedSteps() {
    const steps = treatmentSteps.filter((step) => step.patientId === this.selectedClient.id);
    return steps.length ? steps : treatmentSteps.slice(0, 5);
  }

  get selectedPayments() {
    return patientPayments.filter((payment) => payment.patientId === this.selectedClient.id);
  }
}
