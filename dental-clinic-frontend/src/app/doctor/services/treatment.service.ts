import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Prescription,
  PrescriptionPayload,
  ProcedureCatalogItem,
  ProcedureCatalogPayload,
  ProcedurePayload,
  Treatment,
  TreatmentHistory,
  TreatmentPayload,
  TreatmentPhoto,
  TreatmentPatientSummary,
  TreatmentProcedure,
} from '../models/treatment.models';

@Injectable({ providedIn: 'root' })
export class TreatmentService {
  private readonly http = inject(HttpClient);

  getMyPatients(query = ''): Observable<TreatmentPatientSummary[]> {
    const params = query ? new HttpParams().set('q', query) : undefined;
    return this.http.get<TreatmentPatientSummary[]>('/api/treatments/my-patients', { params });
  }

  getPatientTreatments(patientId: number): Observable<Treatment[]> {
    return this.http.get<Treatment[]>(`/api/patients/${patientId}/treatments`);
  }

  getTreatment(id: number): Observable<Treatment> {
    return this.http.get<Treatment>(`/api/treatments/${id}`);
  }

  createTreatment(patientId: number, payload: TreatmentPayload): Observable<Treatment> {
    return this.http.post<Treatment>(`/api/patients/${patientId}/treatments`, payload);
  }

  updateTreatment(id: number, payload: TreatmentPayload): Observable<Treatment> {
    return this.http.put<Treatment>(`/api/treatments/${id}`, payload);
  }

  deleteTreatment(id: number): Observable<void> {
    return this.http.delete<void>(`/api/treatments/${id}`);
  }

  getProcedures(treatmentId: number): Observable<TreatmentProcedure[]> {
    return this.http.get<TreatmentProcedure[]>(`/api/treatments/${treatmentId}/procedures`);
  }

  addProcedure(treatmentId: number, payload: ProcedurePayload): Observable<TreatmentProcedure> {
    return this.http.post<TreatmentProcedure>(`/api/treatments/${treatmentId}/procedures`, payload);
  }

  updateProcedure(id: number, payload: ProcedurePayload): Observable<TreatmentProcedure> {
    return this.http.put<TreatmentProcedure>(`/api/procedures/${id}`, payload);
  }

  deleteProcedure(id: number): Observable<void> {
    return this.http.delete<void>(`/api/procedures/${id}`);
  }

  searchCatalog(query = '', includeInactive = false): Observable<ProcedureCatalogItem[]> {
    let params = new HttpParams().set('includeInactive', includeInactive);
    if (query) params = params.set('q', query);
    return this.http.get<ProcedureCatalogItem[]>('/api/procedure-catalog', { params });
  }

  createCatalogItem(payload: ProcedureCatalogPayload): Observable<ProcedureCatalogItem> {
    return this.http.post<ProcedureCatalogItem>('/api/procedure-catalog', payload);
  }

  updateCatalogItem(id: number, payload: ProcedureCatalogPayload): Observable<ProcedureCatalogItem> {
    return this.http.put<ProcedureCatalogItem>(`/api/procedure-catalog/${id}`, payload);
  }

  deleteCatalogItem(id: number): Observable<void> {
    return this.http.delete<void>(`/api/procedure-catalog/${id}`);
  }

  getTimeline(treatmentId: number): Observable<TreatmentHistory[]> {
    return this.http.get<TreatmentHistory[]>(`/api/treatments/${treatmentId}/timeline`);
  }

  addHistory(treatmentId: number, payload: Partial<TreatmentHistory>): Observable<TreatmentHistory> {
    return this.http.post<TreatmentHistory>(`/api/treatments/${treatmentId}/history`, payload);
  }

  getPhotos(treatmentId: number): Observable<TreatmentPhoto[]> {
    return this.http.get<TreatmentPhoto[]>(`/api/treatments/${treatmentId}/photos`);
  }

  addPhoto(treatmentId: number, payload: Omit<TreatmentPhoto, 'id' | 'treatmentId' | 'uploadedAt'>): Observable<TreatmentPhoto> {
    return this.http.post<TreatmentPhoto>(`/api/treatments/${treatmentId}/photos`, payload);
  }

  deletePhoto(id: number): Observable<void> {
    return this.http.delete<void>(`/api/photos/${id}`);
  }

  getPrescriptions(treatmentId: number): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`/api/treatments/${treatmentId}/prescriptions`);
  }

  addPrescription(treatmentId: number, payload: PrescriptionPayload): Observable<Prescription> {
    return this.http.post<Prescription>(`/api/treatments/${treatmentId}/prescriptions`, payload);
  }

  updatePrescription(id: number, payload: PrescriptionPayload): Observable<Prescription> {
    return this.http.put<Prescription>(`/api/prescriptions/${id}`, payload);
  }

  deletePrescription(id: number): Observable<void> {
    return this.http.delete<void>(`/api/prescriptions/${id}`);
  }
}
