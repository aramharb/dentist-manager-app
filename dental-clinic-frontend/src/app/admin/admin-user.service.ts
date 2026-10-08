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
  /** The person has not chosen a password yet and still has a valid link. */
  invitationPending: boolean;
}

export interface Invitation {
  userId: number;
  username: string;
  fullName: string;
  /** Path of the one-time link, e.g. /invite/abc. Prefix with the site origin to share it. */
  path: string;
  expiresAt: string;
}

export interface CreatedAccount {
  user: AdminUser;
  invitation: Invitation;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  role: UserRole;
  cabinetId: number | null;
}

export interface MemberRequest {
  username: string;
  fullName: string;
  role: UserRole;
  active?: boolean;
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
  manager: AdminUser | null;
  ownerName: string | null;
  tagline: string | null;
  primaryColor: string;
}

export interface CreatedCabinet {
  cabinet: Cabinet;
  managerInvitation: Invitation;
}

export interface ManagerRequest {
  username: string;
  fullName: string;
}

export interface CabinetRequest {
  name: string;
  code: string;
  address: string | null;
  phoneNumber: string | null;
  email: string | null;
  active?: boolean;
  copyCatalogFromCabinetId?: number | null;
  /** Required when creating a cabinet. */
  manager?: ManagerRequest;
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

  create(request: CreateUserRequest): Observable<CreatedAccount> {
    return this.http.post<CreatedAccount>(ADMIN_USERS_API, request);
  }

  update(id: number, request: UpdateUserRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${ADMIN_USERS_API}/${id}`, request);
  }

  invite(id: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${ADMIN_USERS_API}/${id}/invitation`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class AdminCabinetService {
  private readonly http = inject(HttpClient);

  list(): Observable<Cabinet[]> {
    return this.http.get<Cabinet[]>(ADMIN_CABINETS_API);
  }

  create(request: CabinetRequest): Observable<CreatedCabinet> {
    return this.http.post<CreatedCabinet>(ADMIN_CABINETS_API, request);
  }

  replaceManager(id: number, request: ManagerRequest): Observable<CreatedAccount> {
    return this.http.put<CreatedAccount>(`${ADMIN_CABINETS_API}/${id}/manager`, request);
  }

  update(id: number, request: CabinetRequest): Observable<Cabinet> {
    return this.http.put<Cabinet>(`${ADMIN_CABINETS_API}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN_CABINETS_API}/${id}`);
  }
}

/** Account management of the cabinet manager: only the manager's own cabinet, only doctors and secretaries. */
@Injectable({ providedIn: 'root' })
export class ManagerUserService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/manager/users';

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(this.api);
  }

  create(request: Omit<MemberRequest, 'active'>): Observable<CreatedAccount> {
    return this.http.post<CreatedAccount>(this.api, request);
  }

  update(id: number, request: MemberRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.api}/${id}`, request);
  }

  invite(id: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.api}/${id}/invitation`, {});
  }
}

/** Public endpoints behind the one-time invitation link. */
export interface InvitationPreview {
  username: string;
  fullName: string;
  role: UserRole;
  cabinetName: string | null;
}

@Injectable({ providedIn: 'root' })
export class InvitationService {
  private readonly http = inject(HttpClient);

  preview(token: string): Observable<InvitationPreview> {
    return this.http.get<InvitationPreview>(`/api/invitations/${encodeURIComponent(token)}`);
  }

  accept(token: string, password: string): Observable<void> {
    return this.http.post<void>(`/api/invitations/${encodeURIComponent(token)}/accept`, { password });
  }
}
