import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { SessionService, SessionUser, UserRole, homeFor } from '../core/auth/session.service';

export type { UserRole } from '../core/auth/session.service';

export type LoginRequest = {
  username: string;
  password: string;
};

export type LoginResponse = {
  role: UserRole;
  userId: number;
  username: string;
  fullName: string;
  token: string;
  message?: string;
};

const LOGIN_API_URL = '/api/login';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);

  login(credentials: LoginRequest): Observable<SessionUser> {
    return this.http.post<LoginResponse>(LOGIN_API_URL, credentials).pipe(
      tap((response) => {
        this.session.establish(
          {
            id: response.userId,
            username: response.username,
            fullName: response.fullName,
            role: response.role,
            active: true,
            permissions: [],
          },
          response.token,
        );
      }),
      switchMap(() => this.me()),
      tap((user) => this.session.synchronize(user)),
    );
  }

  me(): Observable<SessionUser> {
    return this.http.get<SessionUser>('/api/auth/me');
  }

  getRedirectUrl(role: UserRole): string {
    return homeFor(role);
  }

}
