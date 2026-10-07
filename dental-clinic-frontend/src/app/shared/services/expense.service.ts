import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Expense, ExpensePayload, ExpenseStatus, FinancialInsight } from '../models/expense.models';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);

  getExpenses(query = '', status?: ExpenseStatus | ''): Observable<Expense[]> {
    let params = new HttpParams();
    if (query) params = params.set('q', query);
    if (status) params = params.set('status', status);
    return this.http.get<Expense[]>('/api/expenses', { params });
  }

  createExpense(payload: ExpensePayload): Observable<Expense> {
    return this.http.post<Expense>('/api/expenses', payload);
  }

  updateExpense(id: number, payload: ExpensePayload): Observable<Expense> {
    return this.http.put<Expense>(`/api/expenses/${id}`, payload);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`/api/expenses/${id}`);
  }

  getDoctorFinancialInsights(): Observable<FinancialInsight> {
    return this.http.get<FinancialInsight>('/api/doctor/insights/financial');
  }
}
