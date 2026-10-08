import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ClientSessionService } from '../client/api/client-api.service';
import { SessionService } from '../core/auth/session.service';

const CLIENT_PUBLIC = ['/api/client/login', '/api/client/register'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api')) {
    return next(request);
  }

  // Client (patient) requests carry the client token; everything else carries the staff token.
  const isClientCall = request.url.startsWith('/api/client/');
  const clientSession = inject(ClientSessionService);
  const session = inject(SessionService);
  const token = isClientCall ? clientSession.token : session.token;
  if (!token || request.url.startsWith('/api/public/')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  ).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && request.url !== '/api/login' && !CLIENT_PUBLIC.includes(request.url)) {
        if (isClientCall) clientSession.clear();
        else session.clear();
      }
      return throwError(() => error);
    }),
  );
};
