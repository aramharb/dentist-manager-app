import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { StaffAction } from './message.service';

@Injectable({ providedIn: 'root' })
export class StaffActionService {
  private readonly http = inject(HttpClient);

  getRecent(): Observable<StaffAction[]> {
    return this.http.get<StaffAction[]>('/api/staff-actions');
  }

  undo(actionId: number): Observable<StaffAction> {
    return this.http.post<StaffAction>(`/api/staff-actions/${actionId}/undo`, null);
  }
}
