import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID, provideZonelessChangeDetection } from '@angular/core';
import { SessionService, SessionUser, TOKEN_STORAGE_KEY } from './session.service';

describe('SessionService', () => {
  const userKey = 'dental-clinic-session';
  const secretary: SessionUser = {
    id: 7,
    username: 'secretary',
    fullName: 'Clinic Secretary',
    role: 'secretaire',
    active: true,
    permissions: [],
  };

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), { provide: PLATFORM_ID, useValue: 'browser' }],
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('stores the active login in the current tab only', () => {
    const service = TestBed.inject(SessionService);

    service.establish(secretary, 'secretary-token');

    expect(sessionStorage.getItem(userKey)).toContain('secretary');
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('secretary-token');
    expect(localStorage.getItem(userKey)).toBeNull();
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
  });

  it('restores the same tab after a page refresh', () => {
    sessionStorage.setItem(userKey, JSON.stringify(secretary));
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'secretary-token');

    const service = TestBed.inject(SessionService);

    expect(service.isAuthenticated).toBeTrue();
    expect(service.currentUser?.role).toBe('secretaire');
    expect(service.token).toBe('secretary-token');
    expect(service.snapshot.status).toBe('restored');
  });

  it('clears only the current tab session on logout', () => {
    const service = TestBed.inject(SessionService);
    service.establish(secretary, 'secretary-token');

    service.clear();

    expect(sessionStorage.getItem(userKey)).toBeNull();
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    expect(service.isAuthenticated).toBeFalse();
  });
});
