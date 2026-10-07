import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject, tap } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';

export type UserRole = 'doctor' | 'secretaire';

export interface ClinicUser {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  online: boolean;
}

export interface PresenceUpdate {
  username: string;
  online: boolean;
}

export interface ScheduleEvent {
  eventType:
    | 'APPOINTMENT_CREATED'
    | 'APPOINTMENT_UPDATED'
    | 'APPOINTMENT_RESCHEDULED'
    | 'APPOINTMENT_CANCELLED'
    | 'APPOINTMENT_DELETED'
    | 'WORKING_HOURS_UPDATED';
  appointmentId?: number | null;
  doctorUserId?: number | null;
  date?: string | null;
  timestamp: string;
}

export interface TreatmentEvent {
  eventType:
    | 'TREATMENT_PLAN_CREATED'
    | 'TREATMENT_PLAN_UPDATED'
    | 'TREATMENT_PLAN_DELETED'
    | 'TREATMENT_CATALOG_UPDATED'
    | 'PROCEDURE_CREATED'
    | 'PROCEDURE_UPDATED'
    | 'PROCEDURE_COMPLETED'
    | 'PROCEDURE_DELETED';
  treatmentId?: number | null;
  patientId?: number | null;
  doctorUserId?: number | null;
  catalogTreatmentId?: number | null;
  timestamp: string;
}

export interface StaffAction {
  id: number;
  actorUserId: number;
  actorName: string;
  actionType: string;
  entityType: 'PATIENT' | 'APPOINTMENT' | 'EXPENSE';
  entityId: number;
  oldValue?: unknown;
  newValue?: unknown;
  description: string;
  timestamp: string;
  status: 'ACTIVE' | 'UNDONE';
  undoable: boolean;
  undoneBy?: string | null;
  undoneAt?: string | null;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderRole: UserRole;
  recipientId?: number | null;
  recipientName?: string | null;
  body: string;
  sentAt: string;
  read: boolean;
  mine: boolean;
}

export interface Conversation {
  id: number;
  title: string;
  participants: ClinicUser[];
  lastMessage?: ChatMessage | null;
  unreadCount: number;
  updatedAt: string;
}

export interface CreateConversationPayload {
  participantIds: number[];
  title?: string | null;
  message?: string | null;
}

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly session = inject(SessionService);
  private readonly http = inject(HttpClient);
  private readonly incomingMessages = new Subject<ChatMessage>();
  private readonly presenceUpdates = new Subject<PresenceUpdate>();
  private readonly incomingStaffActions = new Subject<StaffAction>();
  private readonly incomingScheduleEvents = new Subject<ScheduleEvent>();
  private readonly incomingTreatmentEvents = new Subject<TreatmentEvent>();
  private client?: Client;
  private activeToken?: string;

  readonly messages$ = this.incomingMessages.asObservable();
  readonly presence$ = this.presenceUpdates.asObservable();
  readonly staffActions$ = this.incomingStaffActions.asObservable();
  readonly scheduleEvents$ = this.incomingScheduleEvents.asObservable();
  readonly treatmentEvents$ = this.incomingTreatmentEvents.asObservable();
  readonly connection$ = new BehaviorSubject<'connecting' | 'connected' | 'offline'>('offline');
  readonly notificationsChanged$ = new Subject<void>();

  getUsers(): Observable<ClinicUser[]> {
    return this.http.get<ClinicUser[]>('/api/users');
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>('/api/messages/conversations');
  }

  createConversation(payload: CreateConversationPayload): Observable<Conversation> {
    return this.http.post<Conversation>('/api/messages/conversations', payload);
  }

  getMessages(conversationId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`/api/messages/conversations/${conversationId}`);
  }

  sendMessage(conversationId: number, body: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`/api/messages/conversations/${conversationId}`, { body });
  }

  markRead(conversationId: number): Observable<void> {
    return this.http.post<void>(`/api/messages/conversations/${conversationId}/read`, null).pipe(
      tap(() => this.notificationsChanged$.next()),
    );
  }

  connect(token: string): void {
    if (this.client?.active && this.activeToken === token) return;
    const previousClient = this.client;
    if (previousClient?.active) void previousClient.deactivate();

    this.activeToken = token;
    this.connection$.next('connecting');
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const client = new Client({
      brokerURL: `${protocol}//${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (this.client !== client || this.activeToken !== token) {
          void client.deactivate();
          return;
        }
        client.subscribe('/user/queue/messages', (frame) => {
          this.incomingMessages.next(JSON.parse(frame.body) as ChatMessage);
        });
        client.subscribe('/topic/presence', (frame) => {
          this.presenceUpdates.next(JSON.parse(frame.body) as PresenceUpdate);
        });
        client.subscribe('/user/queue/schedule', (frame) => {
          this.incomingScheduleEvents.next(JSON.parse(frame.body) as ScheduleEvent);
        });
        client.subscribe('/user/queue/treatments', (frame) => {
          this.incomingTreatmentEvents.next(JSON.parse(frame.body) as TreatmentEvent);
        });
        if (this.session.currentUser?.role === 'doctor') {
          client.subscribe('/user/queue/activity', (frame) => {
            this.incomingStaffActions.next(JSON.parse(frame.body) as StaffAction);
          });
        }
        this.connection$.next('connected');
        this.notificationsChanged$.next();
      },
      onWebSocketClose: () => { if (this.client === client) this.connection$.next('offline'); },
      onStompError: () => { if (this.client === client) this.connection$.next('offline'); },
      onWebSocketError: () => { if (this.client === client) this.connection$.next('offline'); },
    });
    this.client = client;
    client.activate();
  }

  disconnect(): void {
    this.connection$.next('offline');
    this.activeToken = undefined;
    const client = this.client;
    this.client = undefined;
    if (client?.active) void client.deactivate();
  }
}
