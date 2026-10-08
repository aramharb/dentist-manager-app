import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface CabinetBranding {
  cabinetId: number;
  name: string;
  ownerName: string | null;
  tagline: string | null;
  primaryColor: string;
  address: string | null;
  phoneNumber: string | null;
  email: string | null;
  hasLogo: boolean;
  hasCover: boolean;
  imageVersion: number;
}

export interface ClientAccount {
  id: number;
  fullName: string;
  phone: string;
}

export interface JoinProfile {
  firstName: string;
  lastName: string;
  gender: 'Male' | 'Female';
  birthDate: string | null;
  address: string | null;
  email: string | null;
  bloodType: string | null;
  allergies: string | null;
  cnamCovered: boolean;
  cnamNumber: string | null;
}

export interface Membership {
  cabinet: CabinetBranding;
  status: 'PENDING' | 'LINKED' | 'REJECTED';
  profile: JoinProfile;
}

export interface DoctorOption {
  id: number;
  fullName: string;
}

export interface SlotDay {
  date: string;
  times: string[];
}

export interface MyRequest {
  id: number;
  cabinetId: number;
  cabinetName: string;
  doctorId: number;
  doctorName: string;
  date: string;
  startTime: string;
  endTime: string;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
  effectiveStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'CANCELLED_BY_CABINET';
  note: string | null;
  rejectReason: string | null;
}

interface AuthResponse {
  token: string;
  account: ClientAccount;
}

const TOKEN_KEY = 'dental-clinic-client-token';
const ACCOUNT_KEY = 'dental-clinic-client-account';

/** Image URL of a cabinet picture (public). The version changes when the image does. */
export function cabinetImageUrl(cabinet: CabinetBranding, kind: 'logo' | 'cover'): string {
  return `/api/public/cabinets/${cabinet.cabinetId}/images/${kind}?v=${cabinet.imageVersion}`;
}

/** Session of a client (patient) account, kept apart from the staff session. */
@Injectable({ providedIn: 'root' })
export class ClientSessionService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly tokenSignal = signal<string | null>(this.read(TOKEN_KEY));
  readonly account = signal<ClientAccount | null>(this.readAccount());

  get token(): string | null {
    return this.tokenSignal();
  }

  get isAuthenticated(): boolean {
    return !!this.tokenSignal() && !!this.account();
  }

  establish(token: string, account: ClientAccount): void {
    this.tokenSignal.set(token);
    this.account.set(account);
    this.write(TOKEN_KEY, token);
    this.write(ACCOUNT_KEY, JSON.stringify(account));
  }

  clear(): void {
    this.tokenSignal.set(null);
    this.account.set(null);
    this.write(TOKEN_KEY, null);
    this.write(ACCOUNT_KEY, null);
  }

  private readAccount(): ClientAccount | null {
    try {
      const raw = this.read(ACCOUNT_KEY);
      return raw ? (JSON.parse(raw) as ClientAccount) : null;
    } catch {
      return null;
    }
  }

  private read(key: string): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      return sessionStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string | null): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      if (value === null) sessionStorage.removeItem(key);
      else sessionStorage.setItem(key, value);
    } catch {
      /* storage unavailable: the session just lasts until reload */
    }
  }
}

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(ClientSessionService);

  cabinets(): Observable<CabinetBranding[]> {
    return this.http.get<CabinetBranding[]>('/api/public/cabinets');
  }

  register(fullName: string, phone: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/client/register', { fullName, phone, password })
      .pipe(tap((response) => this.session.establish(response.token, response.account)));
  }

  login(phone: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/client/login', { phone, password })
      .pipe(tap((response) => this.session.establish(response.token, response.account)));
  }

  memberships(): Observable<Membership[]> {
    return this.http.get<Membership[]>('/api/client/memberships');
  }

  join(cabinetId: number, profile: JoinProfile): Observable<Membership> {
    return this.http.put<Membership>(`/api/client/cabinets/${cabinetId}/membership`, profile);
  }

  doctors(cabinetId: number): Observable<DoctorOption[]> {
    return this.http.get<DoctorOption[]>(`/api/client/cabinets/${cabinetId}/doctors`);
  }

  freeTimes(cabinetId: number, doctorId: number, from: string, days = 7): Observable<SlotDay[]> {
    const params = new HttpParams().set('from', from).set('days', days);
    return this.http.get<SlotDay[]>(`/api/client/cabinets/${cabinetId}/doctors/${doctorId}/free-times`, { params });
  }

  request(cabinetId: number, doctorId: number, date: string, time: string, note: string): Observable<MyRequest> {
    return this.http.post<MyRequest>(`/api/client/cabinets/${cabinetId}/requests`, {
      doctorId,
      date,
      time,
      note: note.trim() || null,
    });
  }

  requests(): Observable<MyRequest[]> {
    return this.http.get<MyRequest[]>('/api/client/requests');
  }

  cancel(requestId: number): Observable<void> {
    return this.http.post<void>(`/api/client/requests/${requestId}/cancel`, {});
  }
}
