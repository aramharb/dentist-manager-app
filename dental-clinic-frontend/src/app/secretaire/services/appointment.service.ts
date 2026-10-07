import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ClinicUser } from '../../shared/services/message.service';
import { Appointment, AppointmentPayload, AppointmentStatus, DoctorWorkingDay, DoctorWorkingHours } from '../models/secretary.models';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly refreshRequests = new Subject<void>();
  readonly refreshRequested$ = this.refreshRequests.asObservable();

  requestRefresh(): void {
    this.refreshRequests.next();
  }

  getAppointments(query = '', date = '', status?: AppointmentStatus | '', doctorId?: number): Observable<Appointment[]> {
    let params = new HttpParams();
    if (query) params = params.set('q', query);
    if (date) params = params.set('date', date);
    if (status) params = params.set('status', status);
    if (doctorId) params = params.set('doctorId', doctorId);
    return this.http.get<Appointment[]>('/api/appointments', { params });
  }

  getAppointment(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`/api/appointments/${id}`);
  }

  createAppointment(payload: AppointmentPayload): Observable<Appointment> {
    return this.http.post<Appointment>('/api/appointments', payload);
  }

  updateAppointment(id: number, payload: AppointmentPayload): Observable<Appointment> {
    return this.http.put<Appointment>(`/api/appointments/${id}`, payload);
  }

  cancelAppointment(id: number): Observable<Appointment> {
    return this.http.patch<Appointment>(`/api/appointments/${id}/cancel`, {});
  }

  deleteAppointment(id: number): Observable<void> {
    return this.http.delete<void>(`/api/appointments/${id}`);
  }

  getDoctors(): Observable<ClinicUser[]> {
    return this.http.get<ClinicUser[]>('/api/users');
  }

  getWorkingHours(doctorId: number): Observable<DoctorWorkingHours> {
    return this.http.get<DoctorWorkingHours>(`/api/doctors/${doctorId}/working-hours`);
  }

  updateWorkingHours(doctorId: number, days: DoctorWorkingDay[]): Observable<DoctorWorkingHours> {
    return this.http.put<DoctorWorkingHours>(`/api/doctors/${doctorId}/working-hours`, { days });
  }
}
