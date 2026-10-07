import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService, UserRole } from './session.service';

export const authenticatedGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);
  return session.isAuthenticated
    ? true
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export function roleGuard(role: UserRole): CanActivateFn {
  return (_route, state) => {
    const session = inject(SessionService);
    const router = inject(Router);
    if (!session.isAuthenticated) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    if (session.currentUser?.role === role) return true;
    return router.createUrlTree([
      session.currentUser?.role === 'doctor' ? '/doctor/dashboard' : '/secretaire/dashboard',
    ]);
  };
}
