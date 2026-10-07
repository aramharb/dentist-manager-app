import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, catchError, distinctUntilChanged, exhaustMap, firstValueFrom, map, of, timer } from 'rxjs';
import { AuthService } from '../../login/auth.service';
import { MessageService } from '../../shared/services/message.service';
import { NotificationService } from '../notifications/notification.service';
import { SessionService } from './session.service';

const SESSION_SYNC_INTERVAL_MS = 45_000;

@Injectable({ providedIn: 'root' })
export class SessionLifecycleService {
  private readonly session = inject(SessionService);
  private readonly auth = inject(AuthService);
  private readonly messages = inject(MessageService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly subscriptions = new Subscription();
  private syncSubscription?: Subscription;
  private started = false;
  private hadIdentifiedUser = false;

  initialize(): Promise<void> | void {
    if (!isPlatformBrowser(this.platformId) || this.started) return;
    this.started = true;
    this.hadIdentifiedUser = !!this.session.currentUser;
    this.bindLifecycle();

    if (this.session.snapshot.status !== 'restored' || !this.session.token) return;
    return firstValueFrom(
      this.auth.me().pipe(
        map((user) => {
          this.session.synchronize(user);
          return undefined;
        }),
        catchError((error: HttpErrorResponse) => {
          if (error.status === 401 || error.status === 403) this.invalidateSession();
          return of(undefined);
        }),
      ),
    );
  }

  logout(): void {
    this.messages.disconnect();
    this.stopSynchronization();
    this.notifications.clear();
    this.session.clear();
    void this.router.navigateByUrl('/login');
  }

  private bindLifecycle(): void {
    this.subscriptions.add(
      this.session.sessionState$
        .pipe(
          map((state) => ({
            authenticated: state.status === 'authenticated' && !!state.user && !!state.token,
            token: state.token,
            userId: state.user?.id,
            role: state.user?.role,
          })),
          distinctUntilChanged(
            (left, right) =>
              left.authenticated === right.authenticated &&
              left.token === right.token &&
              left.userId === right.userId &&
              left.role === right.role,
          ),
        )
        .subscribe((identity) => {
          if (identity.authenticated && identity.token) {
            this.hadIdentifiedUser = true;
            this.messages.connect(identity.token);
            this.startSynchronization();
            this.enforceRoleRoute();
            return;
          }
          this.messages.disconnect();
          this.stopSynchronization();
          this.notifications.clear();
          if (this.hadIdentifiedUser && !this.session.currentUser) void this.router.navigateByUrl('/login');
        }),
    );

    this.subscriptions.add(
      this.messages.messages$.subscribe((message) => {
        if (message.mine) return;
        const route = this.session.currentUser?.role === 'doctor' ? '/doctor/messages' : '/secretaire/messages';
        this.notifications.show({
          kind: 'message',
          title: `New message from ${message.senderName}`,
          message: message.body.length > 140 ? `${message.body.slice(0, 137)}...` : message.body,
          link: `${route}?user=${message.senderId}`,
        });
      }),
    );

    this.subscriptions.add(
      this.messages.staffActions$.subscribe((action) => {
        this.notifications.show({
          kind: 'user',
          title: `Clinic update from ${action.actorName}`,
          message: action.description,
        });
      }),
    );
  }

  private startSynchronization(): void {
    if (this.syncSubscription && !this.syncSubscription.closed) return;
    this.syncSubscription = timer(SESSION_SYNC_INTERVAL_MS, SESSION_SYNC_INTERVAL_MS)
      .pipe(
        exhaustMap(() =>
          this.auth.me().pipe(
            catchError((error: HttpErrorResponse) => {
              if (error.status === 401 || error.status === 403) this.invalidateSession();
              return of(null);
            }),
          ),
        ),
      )
      .subscribe((user) => {
        if (user) this.session.synchronize(user);
      });
  }

  private stopSynchronization(): void {
    this.syncSubscription?.unsubscribe();
    this.syncSubscription = undefined;
  }

  private invalidateSession(): void {
    this.session.clear();
  }

  private enforceRoleRoute(): void {
    const role = this.session.currentUser?.role;
    if (role === 'doctor' && this.router.url.startsWith('/secretaire')) {
      void this.router.navigateByUrl('/doctor/dashboard');
    } else if (role === 'secretaire' && this.router.url.startsWith('/doctor')) {
      void this.router.navigateByUrl('/secretaire/dashboard');
    }
  }
}
