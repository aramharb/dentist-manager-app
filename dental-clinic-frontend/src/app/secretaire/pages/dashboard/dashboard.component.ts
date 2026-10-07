import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, filter, merge, of, switchMap } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { DashboardData, DashboardService } from '../../../shared/services/dashboard.service';
import { MessageService } from '../../../shared/services/message.service';

@Component({
  selector: 'app-secretary-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent {
  private readonly session = inject(SessionService);
  private readonly dashboardService = inject(DashboardService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly manualRefresh$ = new Subject<void>();

  readonly currentUser$ = this.session.currentUser$;
  dashboard?: DashboardData;
  loading = true;
  error = '';

  constructor() {
    merge(
      this.session.currentUser$.pipe(filter((user) => !!user)),
      this.messageService.messages$,
      this.messageService.staffActions$,
      this.messageService.notificationsChanged$,
      this.manualRefresh$,
    )
      .pipe(
        switchMap(() => {
          this.loading = true;
          return this.dashboardService.getMine().pipe(
            catchError(() => {
              this.error = 'Dashboard data is temporarily unavailable.';
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dashboard) => {
        this.loading = false;
        if (dashboard) {
          this.dashboard = dashboard;
          this.error = '';
        }
        this.cdr.markForCheck();
      });
  }

  refresh(): void {
    this.manualRefresh$.next();
  }

  initials(value: string): string {
    return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('') || 'U';
  }
}
