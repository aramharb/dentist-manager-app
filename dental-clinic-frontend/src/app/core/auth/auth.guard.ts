import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService, UserRole, homeFor } from './session.service';

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
    const currentRole = session.currentUser?.role;
    if (currentRole === role) return true;
    return router.parseUrl(currentRole ? homeFor(currentRole) : '/login');
  };
}
