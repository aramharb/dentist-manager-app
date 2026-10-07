import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserRole } from '../core/auth/session.service';

export interface AdminUser {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  cabinetId: number | null;
  cabinetName: string | null;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  role: UserRole;
  password: string;
  cabinetId: number | null;
}

export interface UpdateUserRequest {
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  cabinetId: number | null;
}

export interface CabinetStats {
  doctors: number;
  secretaries: number;
  patients: number;
  appointments: number;
  treatments: number;
}

export interface Cabinet {
  id: number;
  name: string;
  code: string;
  address: string | null;
  phoneNumber: string | null;
  email: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  stats: CabinetStats;
}

export interface CabinetRequest {
  name: string;
  code: string;
  address: string | null;
  phoneNumber: string | null;
  email: string | null;
  active?: boolean;
  copyCatalogFromCabinetId?: number | null;
}

const ADMIN_CABINETS_API = '/api/admin/cabinets';
const ADMIN_USERS_API = '/api/admin/users';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);

  list(cabinetId?: number | null): Observable<AdminUser[]> {
    const params = cabinetId == null ? undefined : new HttpParams().set('cabinetId', cabinetId);
    return this.http.get<AdminUser[]>(ADMIN_USERS_API, { params });
  }

  create(request: CreateUserRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(ADMIN_USERS_API, request);
  }

  update(id: number, request: UpdateUserRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${ADMIN_USERS_API}/${id}`, request);
  }

  resetPassword(id: number, password: string): Observable<void> {
    return this.http.put<void>(`${ADMIN_USERS_API}/${id}/password`, { password });
  }
}

@Injectable({ providedIn: 'root' })
export class AdminCabinetService {
  private readonly http = inject(HttpClient);

  list(): Observable<Cabinet[]> {
    return this.http.get<Cabinet[]>(ADMIN_CABINETS_API);
  }

  create(request: CabinetRequest): Observable<Cabinet> {
    return this.http.post<Cabinet>(ADMIN_CABINETS_API, request);
  }

  update(id: number, request: CabinetRequest): Observable<Cabinet> {
    return this.http.put<Cabinet>(`${ADMIN_CABINETS_API}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN_CABINETS_API}/${id}`);
  }
}
