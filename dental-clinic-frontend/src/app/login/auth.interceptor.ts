import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SessionService } from '../core/auth/session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  if (!request.url.startsWith('/api')) {
    return next(request);
  }

  const token = session.token;
  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  ).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && request.url !== '/api/login') session.clear();
      return throwError(() => error);
    }),
  );
};
