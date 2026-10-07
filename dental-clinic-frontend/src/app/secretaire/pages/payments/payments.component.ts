import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { clients, patientPayments, treatmentCosts } from '../../data/mock-secretary.data';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page fade-in">
      <nav class="breadcrumb">DentaCare Pro / Secretary / Payments</nav>
      <div class="page-title">
        <div><p class="eyebrow">Patient payment</p><h1>Payment desk</h1><p class="page-subtitle">Record demo patient payments against doctor-defined treatment costs.</p></div>
        <button class="primary-button" type="button" (click)="confirmOpen = true">Record payment</button>
      </div>

      <div class="stat-grid">
        <article class="stat-card"><span>Today revenue</span><strong>{{ todayRevenue | currency }}</strong><small>8 receipts</small></article>
        <article class="stat-card cyan"><span>This week</span><strong>{{ weekRevenue | currency }}</strong><small>+12% vs last week</small></article>
        <article class="stat-card mint"><span>This month</span><strong>{{ monthRevenue | currency }}</strong><small>Static demo data</small></article>
        <article class="stat-card amber"><span>Total revenue</span><strong>{{ totalRevenue | currency }}</strong><small>All visible treatments</small></article>
      </div>

      <div class="dashboard-grid">
        <article class="panel">
          <div class="section-heading"><h2>Receive payment</h2><span>Frontend only</span></div>
          <label>Patient<select [(ngModel)]="selectedPatientId"><option *ngFor="let client of clients" [ngValue]="client.id">{{ client.firstName }} {{ client.lastName }}</option></select></label>
          <div class="cost-card">
            <span>Treatment cost defined by doctor</span><strong>{{ selectedCost.finalBalance | currency }}</strong>
            <small>{{ selectedCost.sessions }} sessions, {{ selectedCost.discount | currency }} discount, {{ selectedCost.insuranceCoverage | currency }} insurance</small>
          </div>
          <label>Amount received<input type="number" [(ngModel)]="receivedAmount" /></label>
          <div class="segmented">
            <button *ngFor="let method of methods" type="button" [class.active]="paymentMethod === method" (click)="paymentMethod = method">{{ method }}</button>
          </div>
          <div class="segmented">
            <button type="button" [class.active]="paymentStatus === 'Full'" (click)="paymentStatus = 'Full'">Full</button>
            <button type="button" [class.active]="paymentStatus === 'Partial'" (click)="paymentStatus = 'Partial'">Partial</button>
          </div>
          <div class="balance-grid">
            <span>Total paid<strong>{{ projectedPaid | currency }}</strong></span>
            <span>Remaining balance<strong>{{ remainingBalance | currency }}</strong></span>
          </div>
        </article>

        <article class="panel">
          <div class="section-heading"><h2>Revenue chart</h2><span>Demo</span></div>
          <div class="spark-grid"><i *ngFor="let value of [42,55,48,78,66,92,71,88]" [style.height.%]="value"></i></div>
          <div class="donut-chart"><span>{{ totalRevenue | currency:'USD':'symbol':'1.0-0' }}</span></div>
        </article>
      </div>

      <article class="panel">
        <div class="section-heading"><h2>Complete payment history</h2><input class="search-input compact" [(ngModel)]="search" placeholder="Search payment history" /></div>
        <div class="table payments-table">
          <div class="table-head"><span>Patient</span><span>Treatment</span><span>Method</span><span>Status</span><span>Paid</span><span>Remaining</span><span>Receipt</span></div>
          <div class="table-row" *ngFor="let payment of filteredPayments">
            <strong>{{ payment.patientName }}</strong><span>{{ payment.treatment }}</span><span>{{ payment.method }}</span>
            <span class="pill" [class.ok]="payment.status === 'Full'">{{ payment.status }}</span>
            <span>{{ payment.totalPaid | currency }}</span><span>{{ payment.treatmentCost - payment.totalPaid | currency }}</span><small>{{ payment.receipt }}</small>
          </div>
        </div>
      </article>

      <div class="modal-backdrop" *ngIf="confirmOpen">
        <div class="modal-card"><h2>Confirm payment</h2><p>Demo receipt will be shown locally for {{ selectedClientName }}.</p><div class="action-row"><button class="primary-button" (click)="confirmOpen = false">Confirm</button><button class="ghost-button" (click)="confirmOpen = false">Cancel</button></div></div>
      </div>
    </section>
  `,
  styleUrl: '../dashboard/dashboard.component.css',
})
export class PaymentsComponent {
  clients = clients;
  payments = patientPayments;
  methods = ['Cash', 'Card', 'Bank Transfer'];
  selectedPatientId = 1;
  receivedAmount = 240;
  paymentMethod = 'Card';
  paymentStatus = 'Partial';
  search = '';
  confirmOpen = false;
  todayRevenue = 2140;
  weekRevenue = 8750;
  monthRevenue = 34800;
  totalRevenue = 91240;

  get selectedCost() {
    return treatmentCosts.find((cost) => cost.patientId === this.selectedPatientId) ?? treatmentCosts[0];
  }

  get selectedClientName() {
    const client = clients.find((item) => item.id === this.selectedPatientId);
    return client ? `${client.firstName} ${client.lastName}` : 'patient';
  }

  get projectedPaid() {
    const existing = this.payments.find((payment) => payment.patientId === this.selectedPatientId)?.totalPaid ?? 0;
    return Math.min(this.selectedCost.finalBalance, existing + Number(this.receivedAmount || 0));
  }

  get remainingBalance() {
    return Math.max(0, this.selectedCost.finalBalance - this.projectedPaid);
  }

  get filteredPayments() {
    const value = this.search.toLowerCase();
    return this.payments.filter((payment) => `${payment.patientName} ${payment.treatment} ${payment.method} ${payment.receipt}`.toLowerCase().includes(value));
  }
}
