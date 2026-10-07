import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { clinicExpenses } from '../../data/mock-secretary.data';
import { Expense, ExpensePayload, ExpenseStatus } from '../../../shared/models/expense.models';
import { ExpenseService } from '../../../shared/services/expense.service';
import { SessionService } from '../../../core/auth/session.service';

@Component({
  selector: 'app-expenses',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <section class="page fade-in">
      <nav class="breadcrumb">DentaCare Pro / Secretary / Expenses</nav>
      <div class="page-title">
        <div>
          <p class="eyebrow">Expenses management</p>
          <h1>Clinic expenses</h1>
          <p class="page-subtitle">Secretary entry for clinic expenses. Detailed analytics are reserved for the doctor finance view.</p>
        </div>
        <button type="button" class="primary-button" (click)="startNewExpense()">New expense</button>
      </div>

      <article class="panel">
        <div class="section-heading">
          <h2>{{ selectedExpense ? 'Saved expense details' : 'New expense' }}</h2>
          <span>{{ saving ? 'Saving' : selectedExpense ? 'Expense #' + selectedExpense.id : 'Secretary input' }}</span>
        </div>
        <form [formGroup]="expenseForm" class="form-grid" (ngSubmit)="saveExpense()">
          <label>Date<input type="date" formControlName="expenseDate" /></label>
          <label>Due date<input type="date" formControlName="dueDate" /></label>
          <label>Category<select formControlName="category"><option value="DENTAL_MATERIALS" disabled *ngIf="expenseForm.controls.category.value === 'DENTAL_MATERIALS'">Dental Materials (created from Materials)</option><option *ngFor="let category of categories" [value]="category.value">{{ category.label }}</option></select></label>
          <label>Amount<input type="number" formControlName="amount" /></label>
          <label>Supplier<input formControlName="supplier" /></label>
          <label>Status<select formControlName="status"><option>PAID</option><option>PENDING</option><option>SCHEDULED</option><option>CANCELLED</option></select></label>
          <label>Description<input formControlName="description" /></label>
          <label>Invoice Number (optional)<input formControlName="invoiceNumber" placeholder="INV-0000" /></label>
          <label>Owner<input formControlName="owner" placeholder="Reception, Inventory, Administration" /></label>
          <label>Payment method<input formControlName="paymentMethod" placeholder="Cash, Card, Bank Transfer" /></label>
          <label class="wide">Other / Unexpected Expense<textarea rows="3" formControlName="unexpectedNote" placeholder="Freely describe any unforeseen expense"></textarea></label>
          <label class="check"><input type="checkbox" formControlName="unexpected" /> Unexpected</label>
          <label class="check"><input type="checkbox" formControlName="taxDeductible" /> Tax deductible</label>
          <label class="check"><input type="checkbox" formControlName="recurring" /> Recurring</label>
          <label class="check paid-check"><input type="checkbox" [checked]="expenseForm.controls.status.value === 'PAID'" (change)="setFormPaid($event)" /> Mark as paid</label>
          <div class="form-actions wide">
            <button type="submit" class="primary-button" [disabled]="saving">{{ saving ? 'Saving...' : selectedExpense ? 'Update expense' : 'Save expense' }}</button>
            <button type="button" class="ghost-button" *ngIf="selectedExpense" (click)="startNewExpense()">Cancel selection</button>
          </div>
        </form>
      </article>

      <article class="panel">
        <div class="section-heading">
          <h2>Submitted expenses</h2>
          <span>Details and statistics are visible to doctor only</span>
        </div>
        <div class="table expense-table secretary-entry-table">
          <div class="table-head secretary-expense-row"><span>Date</span><span>Category</span><span>Status</span><span>Amount</span><span>Paid</span></div>
          <div
            class="table-row secretary-expense-row"
            *ngFor="let expense of filtered"
            [class.selected]="selectedExpense?.id === expense.id"
            role="button"
            tabindex="0"
            (click)="selectExpense(expense)"
            (keydown.enter)="selectExpense(expense)"
          >
            <span>{{ expense.expenseDate }}</span>
            <span class="pill">{{ categoryLabel(expense.category) }}</span>
            <span class="status-mark" [class.paid]="expense.status === 'PAID'">{{ expense.status }}</span>
            <strong>{{ expense.amount | currency }}</strong>
            <label class="paid-toggle" (click)="$event.stopPropagation()">
              <input type="checkbox" [checked]="expense.status === 'PAID'" [disabled]="savingExpenseId === expense.id" (change)="markExpensePaid(expense, $event)" />
              <span>{{ expense.status === 'PAID' ? 'Paid' : 'Mark paid' }}</span>
            </label>
          </div>
        </div>
      </article>
      <div class="modal-backdrop" *ngIf="dialogOpen"><div class="modal-card"><h2>Expense saved</h2><p>{{ dialogMessage }}</p><button class="primary-button" (click)="dialogOpen = false">Done</button></div></div>
    </section>
  `,
  styles: [`
    .expense-table .secretary-expense-row { grid-template-columns: .8fr 1fr .8fr .7fr 1fr; }
    .expense-table .table-row { cursor: pointer; transition: background .15s ease, box-shadow .15s ease; }
    .expense-table .table-row:hover, .expense-table .table-row:focus, .expense-table .table-row.selected { background: #f0faff; outline: none; }
    .expense-table .table-row.selected { box-shadow: inset 4px 0 0 var(--primary); }
    .status-mark { font-weight: 800; color: #9a6700; }
    .status-mark.paid { color: #15803d; }
    .paid-toggle { display: inline-flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 800; color: var(--primary-dark); }
    .paid-toggle input, .paid-check input { width: 18px; height: 18px; accent-color: #16a34a; }
    .paid-check { color: #15803d; font-weight: 900; }
    .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 4px; }
    @media (max-width: 760px) { .form-actions { justify-content: stretch; flex-direction: column; } }
  `],
  styleUrl: '../dashboard/dashboard.component.css',
})
export class ExpensesComponent implements OnInit {
  private readonly expenseService = inject(ExpenseService);
  private readonly session = inject(SessionService);
  private readonly fb = inject(FormBuilder);
  private readonly platformId = inject(PLATFORM_ID);

  expenses: Expense[] = [];
  search = '';
  dialogOpen = false;
  dialogMessage = '';
  saving = false;
  savingExpenseId: number | null = null;
  selectedExpense: Expense | null = null;
  categories = [
    { value: 'LABORATORY', label: 'Laboratory' },
    { value: 'MAINTENANCE', label: 'Maintenance' },
    { value: 'ELECTRICITY', label: 'Electricity' },
    { value: 'INTERNET', label: 'Internet' },
    { value: 'WATER', label: 'Water' },
    { value: 'SALARIES', label: 'Salaries' },
    { value: 'TAXES', label: 'Taxes' },
    { value: 'CLEANING', label: 'Cleaning' },
    { value: 'EQUIPMENT', label: 'Equipment' },
    { value: 'MARKETING', label: 'Marketing' },
    { value: 'MISCELLANEOUS', label: 'Miscellaneous' },
    { value: 'UNEXPECTED_MAINTENANCE', label: 'Unexpected Maintenance' },
    { value: 'WATER_BILL', label: 'Water Bill' },
    { value: 'ELECTRICITY_BILL', label: 'Electricity Bill' },
    { value: 'INTERNET_BILL', label: 'Internet Bill' },
    { value: 'PATENTE_BILL', label: 'Patente Bill' },
  ];

  expenseForm = this.fb.nonNullable.group({
    expenseDate: [new Date().toISOString().slice(0, 10), Validators.required],
    dueDate: [''],
    category: ['LABORATORY', Validators.required],
    amount: [185, [Validators.required, Validators.min(0)]],
    supplier: ['Premium Dental Supply'],
    status: ['PENDING' as ExpenseStatus],
    description: ['Emergency anesthetic refill', Validators.required],
    invoiceNumber: [''],
    owner: ['Reception'],
    paymentMethod: ['Cash'],
    unexpected: [false],
    unexpectedNote: [''],
    taxDeductible: [true],
    recurring: [false],
  });

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.expenses = this.mockExpenses();
      return;
    }
    this.loadExpenses();
  }

  loadExpenses(): void {
    this.expenseService.getExpenses(this.search).subscribe({
      next: (expenses) => (this.expenses = expenses),
      error: () => (this.expenses = this.mockExpenses()),
    });
  }

  saveExpense(): void {
    if (this.expenseForm.invalid || this.saving) {
      this.expenseForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const wasEditing = Boolean(this.selectedExpense);
    const value = this.expenseForm.getRawValue();
    const payload: ExpensePayload = {
      expenseDate: value.expenseDate,
      dueDate: value.dueDate || null,
      paidAt: value.status === 'PAID' ? new Date().toISOString() : null,
      category: value.category,
      label: value.description,
      description: value.description,
      supplier: value.supplier,
      invoiceNumber: value.invoiceNumber || null,
      amount: value.amount,
      status: value.status,
      owner: value.owner,
      sourceRole: 'SECRETARY',
      enteredBy: this.session.currentUser?.fullName || this.session.currentUser?.username || 'Secretary',
      unexpected: value.unexpected,
      unexpectedNote: value.unexpectedNote || null,
      paymentMethod: value.paymentMethod,
      taxDeductible: value.taxDeductible,
      recurring: value.recurring,
      attachmentUrl: null,
      billingPeriodMonths: 1,
    };
    const request = this.selectedExpense
      ? this.expenseService.updateExpense(this.selectedExpense.id, payload)
      : this.expenseService.createExpense(payload);
    request.subscribe({
      next: (expense) => {
        this.upsertExpense(expense);
        this.selectExpense(expense);
        this.saving = false;
        this.dialogMessage = wasEditing
          ? 'The saved expense details have been updated.'
          : 'The expense is now available in the doctor finance details.';
        this.dialogOpen = true;
      },
      error: () => (this.saving = false),
    });
  }

  selectExpense(expense: Expense): void {
    this.selectedExpense = expense;
    this.expenseForm.patchValue({
      expenseDate: expense.expenseDate,
      dueDate: expense.dueDate || '',
      category: expense.category,
      amount: expense.amount,
      supplier: expense.supplier || '',
      status: expense.status,
      description: expense.description || expense.label,
      invoiceNumber: expense.invoiceNumber || '',
      owner: expense.owner || '',
      paymentMethod: expense.paymentMethod || '',
      unexpected: expense.unexpected,
      unexpectedNote: expense.unexpectedNote || '',
      taxDeductible: expense.taxDeductible,
      recurring: expense.recurring,
    });
  }

  startNewExpense(): void {
    this.selectedExpense = null;
    this.expenseForm.reset({
      expenseDate: new Date().toISOString().slice(0, 10),
      dueDate: '',
      category: 'LABORATORY',
      amount: 0,
      supplier: '',
      status: 'PENDING',
      description: '',
      invoiceNumber: '',
      owner: 'Reception',
      paymentMethod: 'Cash',
      unexpected: false,
      unexpectedNote: '',
      taxDeductible: true,
      recurring: false,
    });
  }

  setFormPaid(event: Event): void {
    this.expenseForm.controls.status.setValue((event.target as HTMLInputElement).checked ? 'PAID' : 'PENDING');
  }

  markExpensePaid(expense: Expense, event: Event): void {
    const paid = (event.target as HTMLInputElement).checked;
    this.savingExpenseId = expense.id;
    this.expenseService.updateExpense(expense.id, {
      expenseDate: expense.expenseDate,
      dueDate: expense.dueDate || null,
      paidAt: paid ? new Date().toISOString() : null,
      category: expense.category,
      label: expense.label,
      description: expense.description || expense.label,
      supplier: expense.supplier || null,
      invoiceNumber: expense.invoiceNumber || null,
      amount: expense.amount,
      status: paid ? 'PAID' : 'PENDING',
      owner: expense.owner || null,
      sourceRole: expense.sourceRole || 'SECRETARY',
      enteredBy: expense.enteredBy || null,
      unexpected: expense.unexpected,
      unexpectedNote: expense.unexpectedNote || null,
      paymentMethod: expense.paymentMethod || null,
      taxDeductible: expense.taxDeductible,
      recurring: expense.recurring,
      attachmentUrl: expense.attachmentUrl || null,
      billingPeriodMonths: expense.billingPeriodMonths || 1,
    }).subscribe({
      next: (updated) => {
        this.upsertExpense(updated);
        if (this.selectedExpense?.id === updated.id) this.selectExpense(updated);
        this.savingExpenseId = null;
      },
      error: () => {
        (event.target as HTMLInputElement).checked = !paid;
        this.savingExpenseId = null;
      },
    });
  }

  categoryLabel(category: string): string {
    if (category === 'DENTAL_MATERIALS') return 'Dental Materials';
    return this.categories.find((item) => item.value === category)?.label
      ?? category.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private upsertExpense(expense: Expense): void {
    this.expenses = this.expenses.some((item) => item.id === expense.id)
      ? this.expenses.map((item) => item.id === expense.id ? expense : item)
      : [expense, ...this.expenses];
  }

  get filtered() { return this.expenses; }

  private mockExpenses(): Expense[] {
    return clinicExpenses.map((expense) => ({
      id: expense.id,
      expenseDate: expense.date,
      dueDate: expense.date,
      paidAt: null,
      category: this.categoryValue(expense.category),
      label: expense.description,
      description: expense.description,
      supplier: expense.supplier,
      invoiceNumber: expense.invoiceNumber ?? null,
      amount: expense.amount,
      status: 'PENDING',
      owner: 'Reception',
      sourceRole: 'SECRETARY',
      enteredBy: 'Nour Belkacem',
      unexpected: Boolean(expense.unexpectedNote),
      unexpectedNote: expense.unexpectedNote ?? null,
      paymentMethod: 'Cash',
      taxDeductible: true,
      recurring: false,
      attachmentUrl: null,
      billingPeriodMonths: 1,
      createdAt: expense.date,
      updatedAt: expense.date,
    }));
  }

  private categoryValue(category: string): string {
    const normalized = category.trim().toUpperCase().replaceAll(/[^A-Z0-9]+/g, '_');
    if (normalized === 'DENTAL_MATERIALS') return normalized;
    return this.categories.some((item) => item.value === normalized) ? normalized : 'MISCELLANEOUS';
  }
}
