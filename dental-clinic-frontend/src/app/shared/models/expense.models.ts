export type ExpenseStatus = 'PAID' | 'PENDING' | 'SCHEDULED' | 'CANCELLED';
export type ExpenseSourceRole = 'SECRETARY' | 'DOCTOR' | 'SYSTEM';

export interface Expense {
  id: number;
  expenseDate: string;
  dueDate?: string | null;
  paidAt?: string | null;
  category: string;
  label: string;
  description?: string | null;
  supplier?: string | null;
  invoiceNumber?: string | null;
  amount: number;
  status: ExpenseStatus;
  owner?: string | null;
  sourceRole: ExpenseSourceRole;
  enteredBy?: string | null;
  unexpected: boolean;
  unexpectedNote?: string | null;
  paymentMethod?: string | null;
  taxDeductible: boolean;
  recurring: boolean;
  attachmentUrl?: string | null;
  billingPeriodMonths?: number;
  createdAt: string;
  updatedAt: string;
}

export type ExpensePayload = Omit<Expense, 'id' | 'createdAt' | 'updatedAt'>;

export interface StatCard {
  label: string;
  value: string;
  hint: string;
}

export interface CategoryTotal {
  category: string;
  amount: number;
}

export interface FinancialInsight {
  grossRevenue: number;
  collectedRevenue: number;
  monthlyExpenses: number;
  pendingExpenses: number;
  scheduledExpenses: number;
  netIncome: number;
  expenseCount: number;
  unexpectedExpenseCount: number;
  stats: StatCard[];
  expensesByCategory: CategoryTotal[];
}
