import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService, AppNotification } from './notification.service';

@Component({
  selector: 'app-notification-container',
  standalone: true,
  template: `
    <div class="notification-stack" aria-live="polite" aria-label="Notifications">
      @for (notification of service.notifications(); track notification.id) {
        <article class="app-toast" [class.clickable]="!!notification.link" (click)="open(notification)">
          <div>
            <strong>{{ notification.title }}</strong>
            <p>{{ notification.message }}</p>
          </div>
          <button type="button" aria-label="Dismiss notification" (click)="dismiss($event, notification.id)">×</button>
        </article>
      }
    </div>
  `,
  styles: `
    .notification-stack { position: fixed; z-index: 2000; right: 20px; top: 20px; display: grid; gap: 10px; width: min(380px, calc(100vw - 32px)); pointer-events: none; }
    .app-toast { pointer-events: auto; display: flex; justify-content: space-between; gap: 16px; padding: 16px 18px; color: #0f172a; background: rgba(255,255,255,.98); border: 1px solid #bae6fd; border-left: 4px solid #0ea5e9; border-radius: 14px; box-shadow: 0 18px 45px rgba(15,23,42,.18); animation: toast-in .18s ease-out; }
    .app-toast.clickable { cursor: pointer; }
    .app-toast strong { display: block; margin-bottom: 4px; }
    .app-toast p { margin: 0; color: #475569; line-height: 1.4; overflow-wrap: anywhere; }
    .app-toast button { align-self: flex-start; border: 0; background: transparent; color: #64748b; font-size: 20px; cursor: pointer; }
    @keyframes toast-in { from { opacity: 0; transform: translateY(-8px); } }
  `,
})
export class NotificationContainerComponent {
  readonly service = inject(NotificationService);
  private readonly router = inject(Router);

  open(notification: AppNotification): void {
    if (notification.link) void this.router.navigateByUrl(notification.link);
    this.service.dismiss(notification.id);
  }

  dismiss(event: MouseEvent, id: number): void {
    event.stopPropagation();
    this.service.dismiss(id);
  }
}
