import { Injectable, signal } from '@angular/core';

export type NotificationKind = 'message' | 'appointment' | 'system' | 'user';

export interface AppNotification {
  id: number;
  kind: NotificationKind;
  title: string;
  message: string;
  link?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 1;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  readonly notifications = signal<AppNotification[]>([]);

  show(notification: Omit<AppNotification, 'id'>, durationMs = 6500): number {
    const id = this.nextId++;
    this.notifications.update((items) => [...items, { ...notification, id }]);
    this.timers.set(id, setTimeout(() => this.dismiss(id), durationMs));
    return id;
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer) clearTimeout(timer);
    this.timers.delete(id);
    this.notifications.update((items) => items.filter((item) => item.id !== id));
  }

  clear(): void {
    this.timers.forEach((timer) => clearTimeout(timer));
    this.timers.clear();
    this.notifications.set([]);
  }
}
