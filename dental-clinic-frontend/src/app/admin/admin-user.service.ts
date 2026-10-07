import { HttpClient } from '@angular/common/http';
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
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  role: UserRole;
  password: string;
}

export interface UpdateUserRequest {
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
}

const ADMIN_USERS_API = '/api/admin/users';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(ADMIN_USERS_API);
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
