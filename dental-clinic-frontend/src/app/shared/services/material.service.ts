import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { InventoryMaterial, MaterialPayload } from '../models/material.models';

@Injectable({ providedIn: 'root' })
export class MaterialService {
  private readonly http = inject(HttpClient);

  getMaterials(): Observable<InventoryMaterial[]> {
    return this.http.get<InventoryMaterial[]>('/api/materials');
  }

  createMaterial(payload: MaterialPayload): Observable<InventoryMaterial> {
    return this.http.post<InventoryMaterial>('/api/materials', payload);
  }

  updateMaterial(id: number, payload: MaterialPayload): Observable<InventoryMaterial> {
    return this.http.put<InventoryMaterial>(`/api/materials/${id}`, payload);
  }
}
