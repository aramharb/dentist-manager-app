import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { BehaviorSubject, distinctUntilChanged, map } from 'rxjs';

export type UserRole = 'doctor' | 'secretaire';
export type SessionStatus = 'anonymous' | 'restored' | 'authenticated';

export interface SessionUser {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  permissions: string[];
}

export interface SessionState {
  user: SessionUser | null;
  token: string | null;
  status: SessionStatus;
}

const SESSION_STORAGE_KEY = 'dental-clinic-session';
export const TOKEN_STORAGE_KEY = 'dental-clinic-token';
const LEGACY_KEYS = [
  'dental-clinic-role',
  'dental-clinic-user-id',
  'dental-clinic-username',
  'dental-clinic-full-name',
];

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly stateSubject = new BehaviorSubject<SessionState>(this.restore());

  readonly sessionState$ = this.stateSubject.asObservable();
  readonly currentUser$ = this.sessionState$.pipe(
    map((state) => state.user),
    distinctUntilChanged((left, right) => JSON.stringify(left) === JSON.stringify(right)),
  );
  readonly isAuthenticated$ = this.sessionState$.pipe(
    map((state) => state.status !== 'anonymous' && !!state.user && !!state.token),
    distinctUntilChanged(),
  );
  readonly userRole$ = this.currentUser$.pipe(
    map((user) => user?.role ?? null),
    distinctUntilChanged(),
  );

  get snapshot(): SessionState {
    return this.stateSubject.value;
  }

  get currentUser(): SessionUser | null {
    return this.snapshot.user;
  }

  get token(): string | null {
    return this.snapshot.token;
  }

  get isAuthenticated(): boolean {
    return this.snapshot.status !== 'anonymous' && !!this.currentUser && !!this.token;
  }

  establish(user: SessionUser, token: string): void {
    this.update(user, token, 'authenticated');
  }

  synchronize(user: SessionUser): void {
    const token = this.token;
    if (!token) return;
    this.update(user, token, 'authenticated');
  }

  clear(): void {
    if (this.isBrowser()) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(SESSION_STORAGE_KEY);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
    }
    this.stateSubject.next({ user: null, token: null, status: 'anonymous' });
  }

  hasPermission(permission: string): boolean {
    return this.currentUser?.permissions.includes(permission) ?? false;
  }

  private update(user: SessionUser, token: string, status: SessionStatus): void {
    const state = { user, token, status } satisfies SessionState;
    if (this.isBrowser()) {
      sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(user));
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
      localStorage.removeItem(SESSION_STORAGE_KEY);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
    }
    this.stateSubject.next(state);
  }

  private restore(): SessionState {
    if (!this.isBrowser()) return { user: null, token: null, status: 'anonymous' };

    const tabSession = this.readSession(sessionStorage);
    if (tabSession) return tabSession;

    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);

    // One-time migration from the former cross-tab storage. Only migrate when
    // the cached role matches the current workspace so a doctor can never take
    // over an already-open secretary tab (or the reverse) during deployment.
    const legacySession = this.readSession(localStorage);
    if (legacySession && this.roleMatchesCurrentWorkspace(legacySession.user!.role)) {
      sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(legacySession.user));
      sessionStorage.setItem(TOKEN_STORAGE_KEY, legacySession.token!);
      localStorage.removeItem(SESSION_STORAGE_KEY);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
      return legacySession;
    }

    if (!legacySession) {
      localStorage.removeItem(SESSION_STORAGE_KEY);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
    }
    return { user: null, token: null, status: 'anonymous' };
  }

  private readSession(storage: Storage): SessionState | null {
    try {
      const rawUser = storage.getItem(SESSION_STORAGE_KEY);
      const token = storage.getItem(TOKEN_STORAGE_KEY);
      if (!rawUser || !token) return null;
      const user = JSON.parse(rawUser) as SessionUser;
      if (!user.id || !user.username || !user.fullName || !['doctor', 'secretaire'].includes(user.role)) {
        throw new Error('Invalid cached session');
      }
      return { user, token, status: 'restored' };
    } catch {
      storage.removeItem(SESSION_STORAGE_KEY);
      storage.removeItem(TOKEN_STORAGE_KEY);
      return null;
    }
  }

  private roleMatchesCurrentWorkspace(role: UserRole): boolean {
    const path = window.location.pathname.toLowerCase();
    if (path.startsWith('/doctor')) return role === 'doctor';
    if (path.startsWith('/secretaire')) return role === 'secretaire';
    return false;
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
}
