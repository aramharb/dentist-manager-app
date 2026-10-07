import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Patient, PatientPayload, ProcedureCatalogItem } from '../models/secretary.models';
import { ClinicUser } from '../../shared/services/message.service';

const PATIENTS_API_URL = '/api/patients';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);

  getPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(PATIENTS_API_URL);
  }

  getPatient(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${PATIENTS_API_URL}/${id}`);
  }

  createPatient(payload: PatientPayload): Observable<Patient> {
    return this.http.post<Patient>(PATIENTS_API_URL, payload);
  }

  updatePatient(id: number, payload: PatientPayload): Observable<Patient> {
    return this.http.put<Patient>(`${PATIENTS_API_URL}/${id}`, payload);
  }

  deletePatient(id: number): Observable<void> {
    return this.http.delete<void>(`${PATIENTS_API_URL}/${id}`);
  }

  getTreatmentCatalog(): Observable<ProcedureCatalogItem[]> {
    return this.http.get<ProcedureCatalogItem[]>('/api/procedure-catalog');
  }

  getDoctors(): Observable<ClinicUser[]> {
    return this.http.get<ClinicUser[]>('/api/users');
  }
}
