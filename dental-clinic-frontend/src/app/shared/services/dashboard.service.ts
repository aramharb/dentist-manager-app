import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SessionUser } from '../../core/auth/session.service';

export interface DashboardAppointment {
  id: number;
  patientId: number;
  patientName: string;
  date: string;
  time: string;
  providerName: string;
  status: string;
}

export interface DashboardData {
  user: SessionUser;
  date: string;
  todayAppointments: number;
  upcomingAppointments: number;
  waitingPatients: number;
  totalPatients: number;
  treatmentsInProgress: number;
  unreadMessages: number;
  appointments: DashboardAppointment[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getMine(): Observable<DashboardData> {
    return this.http.get<DashboardData>('/api/dashboard/me');
  }
}
