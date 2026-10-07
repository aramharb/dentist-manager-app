import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { doctorExpenses, doctorTreatments, labelFr, netIncome, totalExpenses, totalRevenue } from '../doctor-dashboard.data';
import { Expense, FinancialInsight } from '../../shared/models/expense.models';
import { ExpenseService } from '../../shared/services/expense.service';
import { SessionService } from '../../core/auth/session.service';

@Component({
  selector: 'app-doctor-money',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <section class="dashboard-grid">
      <article class="panel">
        <div class="section-heading"><h2>Finances</h2><span>Net {{ insight.netIncome | currency }}</span></div>
        <div class="bar-chart">
          <div *ngFor="let item of treatments" [style.--bar]="item.count + '%'">
            <span>{{ item.treatment }}</span><i [style.background]="item.color"></i><strong>{{ item.revenue | currency }}</strong>
          </div>
        </div>
        <div class="metric-grid owner-metrics">
          <article><span>Collected</span><strong>{{ insight.collectedRevenue | currency }}</strong></article>
          <article><span>Expenses</span><strong>{{ insight.monthlyExpenses | currency }}</strong></article>
          <article><span>Pending</span><strong>{{ insight.pendingExpenses | currency }}</strong></article>
          <article><span>Unexpected</span><strong>{{ insight.unexpectedExpenseCount }}</strong></article>
        </div>
        <div class="table">
          <div class="table-head money"><span>Categorie</span><span>Libelle</span><span>Date</span><span>Statut</span><span>Source</span><span>Montant</span></div>
          <div class="table-row money" *ngFor="let expense of expenses">
            <span>{{ expense.category }}</span>
            <span>{{ expense.label }}</span>
            <span>{{ expense.expenseDate }}</span>
            <span>{{ labelFr(expense.status) }}</span>
            <span>{{ expense.sourceRole }}</span>
            <strong>{{ expense.amount | currency }}</strong>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="owner-mini" *ngIf="currentUser$ | async as user"><div><strong>{{ user.fullName }}</strong><small>Doctor workspace</small></div></div>
        <div class="section-heading"><h2>Doctor expense entry</h2><span>Owner only</span></div>
        <form [formGroup]="doctorExpenseForm" class="doctor-expense-form" (ngSubmit)="addDoctorExpense()">
          <label>Date<input type="date" formControlName="expenseDate" /></label>
          <label>Category<input formControlName="category" /></label>
          <label>Amount<input type="number" formControlName="amount" /></label>
          <label>Status<select formControlName="status"><option>PAID</option><option>PENDING</option><option>SCHEDULED</option></select></label>
          <label class="wide">Description<textarea rows="4" formControlName="description"></textarea></label>
          <button type="submit">Ajouter depense</button>
        </form>

        <div class="section-heading"><h2>Previsions</h2><span>Vue proprietaire</span></div>
        <label>Objectif mensuel<input [(ngModel)]="targetIncome" /></label>
        <label>Plafond des depenses<input [(ngModel)]="expenseCeiling" /></label>
        <label>Budget equipement<input [(ngModel)]="equipmentBudget" /></label>
        <div class="row-card"><div><strong>{{ insight.grossRevenue | currency }} brut</strong><p>Revenus des traitements en base.</p></div></div>
        <div class="row-card"><div><strong>{{ insight.monthlyExpenses | currency }} depenses</strong><p>Entries from secretary plus doctor owner entries.</p></div></div>
        <div class="category-list">
          <div *ngFor="let item of insight.expensesByCategory"><span>{{ item.category }}</span><strong>{{ item.amount | currency }}</strong></div>
        </div>
        <div class="action-row"><button type="button" (click)="loadFinancials()">Mettre a jour</button><button type="button" class="ghost-button">Telecharger rapport</button></div>
      </article>
    </section>
  `,
  styles: [`
    .money { grid-template-columns: .8fr 1.5fr .7fr .7fr .7fr .7fr; }
    .owner-metrics { grid-template-columns: repeat(4, minmax(0, 1fr)); margin: 14px 0; }
    .owner-metrics article, .category-list div { border: 1px solid var(--line); border-radius: 8px; padding: 12px; background: #f8fcff; }
    .owner-metrics span, .category-list span { color: var(--muted); }
    .owner-metrics strong, .category-list strong { display: block; margin-top: 5px; }
    .doctor-expense-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-bottom: 18px; }
    .doctor-expense-form .wide { grid-column: 1 / -1; }
    .doctor-expense-form input, .doctor-expense-form select, .doctor-expense-form textarea { width: 100%; box-sizing: border-box; border: 1px solid var(--line); border-radius: 8px; padding: 10px 12px; }
    .category-list { display: grid; gap: 8px; margin: 12px 0; }
    @media (max-width: 900px) { .doctor-expense-form, .owner-metrics { grid-template-columns: 1fr; } }
  `],
  styleUrl: './doctor-page.css',
})
export class DoctorMoneyComponent implements OnInit {
  private readonly expenseService = inject(ExpenseService);
  private readonly fb = inject(FormBuilder);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly session = inject(SessionService);
  readonly currentUser$ = this.session.currentUser$;

  treatments = doctorTreatments;
  expenses: Expense[] = [];
  insight: FinancialInsight = this.mockInsight();
  labelFr = labelFr;
  targetIncome = '$48,000';
  expenseCeiling = '$6,000';
  equipmentBudget = '$3,500';

  doctorExpenseForm = this.fb.nonNullable.group({
    expenseDate: [new Date().toISOString().slice(0, 10), Validators.required],
    category: ['MISCELLANEOUS', Validators.required],
    amount: [250, [Validators.required, Validators.min(0)]],
    status: ['PENDING'],
    description: ['Owner-approved clinical expense', Validators.required],
  });

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.expenses = this.mockExpenses();
      return;
    }
    this.loadFinancials();
  }

  loadFinancials(): void {
    this.expenseService.getDoctorFinancialInsights().subscribe({ next: (insight) => (this.insight = insight), error: () => (this.insight = this.mockInsight()) });
    this.expenseService.getExpenses().subscribe({ next: (expenses) => (this.expenses = expenses), error: () => (this.expenses = this.mockExpenses()) });
  }

  addDoctorExpense(): void {
    if (this.doctorExpenseForm.invalid) return;
    const value = this.doctorExpenseForm.getRawValue();
    this.expenseService.createExpense({
      expenseDate: value.expenseDate,
      dueDate: value.expenseDate,
      paidAt: value.status === 'PAID' ? new Date().toISOString() : null,
      category: value.category,
      label: value.description,
      description: value.description,
      supplier: null,
      invoiceNumber: null,
      amount: value.amount,
      status: value.status as any,
      owner: 'Doctor',
      sourceRole: 'DOCTOR',
      enteredBy: this.session.currentUser?.fullName || this.session.currentUser?.username || 'Doctor',
      unexpected: false,
      unexpectedNote: null,
      paymentMethod: 'Bank Transfer',
      taxDeductible: true,
      recurring: false,
      attachmentUrl: null,
      billingPeriodMonths: 1,
    }).subscribe(() => this.loadFinancials());
  }

  private mockInsight(): FinancialInsight {
    return {
      grossRevenue: totalRevenue,
      collectedRevenue: totalRevenue,
      monthlyExpenses: totalExpenses,
      pendingExpenses: totalExpenses,
      scheduledExpenses: 0,
      netIncome,
      expenseCount: doctorExpenses.length,
      unexpectedExpenseCount: 1,
      stats: [],
      expensesByCategory: doctorExpenses.map((expense) => ({ category: expense.category, amount: expense.amount })),
    };
  }

  private mockExpenses(): Expense[] {
    return doctorExpenses.map((expense) => ({
      id: expense.id,
      expenseDate: expense.dueDate,
      dueDate: expense.dueDate,
      paidAt: null,
      category: expense.category,
      label: expense.label,
      description: expense.label,
      supplier: null,
      invoiceNumber: null,
      amount: expense.amount,
      status: expense.status.toUpperCase() as any,
      owner: expense.owner,
      sourceRole: 'SECRETARY',
      enteredBy: 'Nour Belkacem',
      unexpected: false,
      unexpectedNote: null,
      paymentMethod: null,
      taxDeductible: true,
      recurring: false,
      attachmentUrl: null,
      billingPeriodMonths: 1,
      createdAt: expense.dueDate,
      updatedAt: expense.dueDate,
    }));
  }
}
