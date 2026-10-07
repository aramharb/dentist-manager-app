import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, PLATFORM_ID, ViewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { ChatMessage, ClinicUser, Conversation, MessageService, PresenceUpdate } from '../../../shared/services/message.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page fade-in">
      <nav class="breadcrumb">DentaCare Pro / Internal messaging</nav>
      <div class="page-title">
        <div>
          <p class="eyebrow">Messages</p>
          <h1>Team messages</h1>
          <p class="page-subtitle">Choose a clinic user, send a message, and the saved thread appears when they log in.</p>
        </div>
      </div>

      <div class="notice error" *ngIf="error">{{ error }}</div>
      <div class="notice" *ngIf="loading">Loading messages...</div>

      <div class="messenger-shell">
        <aside class="people-panel">
          <div class="panel-head">
            <div>
              <span>All users</span>
              <strong>{{ visibleUsers.length }}</strong>
            </div>
          </div>
          <input class="people-search" [(ngModel)]="query" placeholder="Search team members" aria-label="Search team members" />
          <p class="connection-label" role="status">{{ connection === 'connected' ? 'Connected · Live updates' : 'Reconnecting · Messages can still be sent' }}</p>

          <button
            type="button"
            class="person-card"
            *ngFor="let user of visibleUsers"
            [class.active]="user.id === selectedUser?.id"
            [disabled]="user.id === currentUserId"
            (click)="selectUser(user)">
            <span class="avatar" [class.online]="user.online">{{ initials(user.fullName) }}</span>
            <span class="person-copy">
              <strong>{{ user.fullName }}{{ user.id === currentUserId ? ' (You)' : '' }}</strong>
              <small>
                {{ user.role }}
                <span class="presence" [class.online]="user.online">
                  {{ user.online ? 'Online' : 'Offline' }}
                </span>
              </small>
              <em>{{ lastMessageFor(user)?.body || 'No messages yet' }}</em>
            </span>
            <i *ngIf="unreadFor(user)">{{ unreadFor(user) }}</i>
          </button>
        </aside>

        <article class="chat-panel">
          <header class="chat-head">
            <span class="avatar large" [class.online]="selectedUser?.online">
              {{ selectedUser ? initials(selectedUser.fullName) : 'U' }}
            </span>
            <div>
              <p *ngIf="selectedUser">
                {{ selectedUser.role }} · {{ selectedUser.online ? 'Online' : 'Offline' }}
              </p>
              <p *ngIf="!selectedUser">Select a user</p>
              <h2>{{ selectedUser?.fullName || 'No recipient selected' }}</h2>
            </div>
          </header>

          <div #messageStack class="message-stack" role="log" aria-live="polite" *ngIf="selectedUser; else chooseUser">
            <p *ngIf="threadLoading" class="connection-label">Loading conversation…</p>
            <div class="bubble" *ngFor="let message of messages" [class.mine]="message.mine">
              <p>{{ message.body }}</p>
              <small>{{ message.mine ? 'Sent' : message.senderName }} · {{ message.sentAt | date:'short' }}</small>
            </div>
            <div class="empty-thread" *ngIf="!messages.length && !threadLoading">
              <strong>No messages yet</strong>
              <span>Write the first message to {{ selectedUser.fullName }}.</span>
            </div>
          </div>

          <ng-template #chooseUser>
            <div class="empty-thread">
              <strong>Choose a user</strong>
              <span>Pick a doctor or secretary from the list.</span>
            </div>
          </ng-template>

          <form class="composer" (ngSubmit)="sendMessage()">
            <textarea [(ngModel)]="draft" name="draft" rows="2" maxlength="4000" [disabled]="!selectedUser"
              (keydown)="composerKeydown($event)" aria-label="Message"
              [placeholder]="selectedUser ? 'Write a message to ' + selectedUser.fullName + '…' : 'Select a team member to start messaging'"></textarea>
            <button class="primary-button" type="submit" [disabled]="!selectedUser || !draft.trim() || sending || threadLoading || loading">
              <span>{{ sending ? 'Sending…' : 'Send message' }}</span>
              <span class="send-arrow" aria-hidden="true">→</span>
            </button>
            <small class="composer-hint">
              <span>Press Enter to send · Shift + Enter for a new line</span>
              <span>{{ draft.length }} / 4000</span>
            </small>
          </form>
        </article>
      </div>
    </section>
  `,
  styleUrl: './messages.component.css',
})
export class MessagesComponent implements OnInit, OnDestroy {
  @ViewChild('messageStack') private messageStack?: ElementRef<HTMLDivElement>;

  private readonly messageService = inject(MessageService);
  private readonly session = inject(SessionService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  query = '';
  connection = 'connecting';
  threadLoading = false;
  private threadRequest?: Subscription;
  private sendingTo?: number;
  private sentDraft = '';
  private drafts = new Map<number, string>();
  private readonly visibilityListener = () => {
    if (!document.hidden && this.selectedConversation) this.markRead(this.selectedConversation.id);
  };

  currentUserId: number | null = null;
  users: ClinicUser[] = [];
  conversations: Conversation[] = [];
  messages: ChatMessage[] = [];
  selectedUser?: ClinicUser;
  selectedConversation?: Conversation;
  draft = '';
  loading = false;
  sending = false;
  error = '';
  private readonly realtimeSubscriptions = new Subscription();

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.currentUserId = this.session.currentUser?.id ?? null;
    if (!this.currentUserId) {
      this.error = 'Please log in again to use messages.';
      return;
    }
    document.addEventListener('visibilitychange', this.visibilityListener);
    this.realtimeSubscriptions.add(this.messageService.connection$.subscribe((state) => {
      this.connection = state;
      if (state === 'connected') {
        this.loadUsers();
        this.loadConversations(undefined, true);
      }
      this.cdr.markForCheck();
    }));
    this.realtimeSubscriptions.add(this.route.queryParamMap.subscribe(() => {
      const id = Number(this.route.snapshot.queryParamMap.get('user'));
      const user = this.users.find(item => item.id === id);
      if (user) this.selectUser(user);
    }));
    this.realtimeSubscriptions.add(
      this.messageService.messages$.subscribe((message) => this.receiveMessage(message)),
    );
    this.realtimeSubscriptions.add(
      this.messageService.presence$.subscribe((update) => this.updatePresence(update)),
    );
    this.loadUsers();
    this.loadConversations();
  }

  ngOnDestroy(): void {
    this.threadRequest?.unsubscribe();
    if (isPlatformBrowser(this.platformId)) document.removeEventListener('visibilitychange', this.visibilityListener);
    this.realtimeSubscriptions.unsubscribe();
  }

  get visibleUsers(): ClinicUser[] {
    return this.users.filter(user => `${user.fullName} ${user.username} ${user.role}`.toLowerCase().includes(this.query.toLowerCase()))
      .sort((a, b) => Number(b.online) - Number(a.online) || a.fullName.localeCompare(b.fullName));
  }

  selectUser(user: ClinicUser): void {
    if (user.id === this.currentUserId) return;
    if (this.selectedUser) this.drafts.set(this.selectedUser.id, this.draft);
    this.selectedUser = user;
    this.draft = this.drafts.get(user.id) || '';
    this.openDirectThread(user);
  }

  sendMessage(): void {
    if (!this.currentUserId || !this.selectedUser || !this.draft.trim() || this.sending || this.loading || this.threadLoading) return;
    this.sending = true;
    this.error = '';
    const body = this.draft.trim();
    this.sendingTo = this.selectedUser.id;
    this.sentDraft = this.draft;
    if (this.selectedConversation) {
      this.messageService.sendMessage(this.selectedConversation.id, body).subscribe({
        next: (message) => this.afterSend(message),
        error: (error) => this.sendFailed(error),
      });
      return;
    }

    this.messageService.createConversation({
      participantIds: [this.selectedUser.id],
      message: body,
    }).subscribe({
      next: (conversation) => {
        this.conversations = [conversation, ...this.conversations.filter(item => item.id !== conversation.id)];
        if (this.selectedUser?.id === this.sendingTo) this.selectedConversation = conversation;
        this.afterSend(conversation.lastMessage || undefined);
      },
      error: (error) => this.sendFailed(error),
    });
  }

  unreadFor(user: ClinicUser): number {
    return this.directConversationFor(user)?.unreadCount ?? 0;
  }

  lastMessageFor(user: ClinicUser): ChatMessage | null | undefined {
    return this.directConversationFor(user)?.lastMessage;
  }

  initials(value: string): string {
    return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join('') || 'U';
  }

  private loadUsers(): void {
    this.messageService.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        if (this.selectedUser) this.selectedUser = users.find(user => user.id === this.selectedUser?.id);
        const target = Number(this.route.snapshot.queryParamMap.get('user'));
        if (!this.selectedUser && target) {
          const user = users.find(item => item.id === target);
          if (user) this.selectUser(user);
        }
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Unable to load clinic users. Please refresh to retry.'; this.cdr.markForCheck(); },
    });
  }

  private loadConversations(reopenUserId?: number, silent = false): void {
    if (!this.currentUserId) return;
    if (!silent) {
      this.loading = true;
    }
    this.messageService.getConversations().subscribe({
      next: (conversations) => {
        this.conversations = conversations;
        this.loading = false;
        this.cdr.markForCheck();
        const userToOpen = reopenUserId
          ? this.users.find((user) => user.id === reopenUserId)
          : this.selectedUser;
        const nextConversation = userToOpen ? this.directConversationFor(userToOpen) : undefined;
        if (userToOpen && (!silent || nextConversation?.id !== this.selectedConversation?.id)) this.openDirectThread(userToOpen);
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load saved messages.';
        this.cdr.markForCheck();
      },
    });
  }

  private openDirectThread(user: ClinicUser): void {
    if (!this.currentUserId) return;
    this.selectedConversation = this.directConversationFor(user);
    this.threadRequest?.unsubscribe();
    this.threadLoading = false;
    if (!this.selectedConversation) {
      this.messages = [];
      return;
    }
    const conversationId = this.selectedConversation.id;
    this.messages = [];
    this.threadLoading = true;
    this.threadRequest = this.messageService.getMessages(conversationId).subscribe({
      next: (messages) => {
        if (this.selectedUser?.id !== user.id) return;
        this.threadLoading = false;
        this.messages = [...new Map([...messages, ...this.messages].map(message => [message.id, message])).values()]
          .sort((a, b) => a.id - b.id);
        this.cdr.markForCheck();
        this.scrollToLatestMessage();
        if (!document.hidden) this.messageService.markRead(conversationId).subscribe({
          next: () => {
            this.conversations = this.conversations.map((conversation) =>
              conversation.id === conversationId ? { ...conversation, unreadCount: 0 } : conversation,
            );
            this.cdr.markForCheck();
          },
          error: () => {},
        });
      },
      error: () => { this.threadLoading = false; this.error = 'Unable to load this conversation. Select the recipient to retry.'; this.cdr.markForCheck(); },
    });
  }

  private directConversationFor(user: ClinicUser): Conversation | undefined {
    return this.conversations.find((conversation) =>
      conversation.participants.length === 2
      && conversation.participants.some((participant) => participant.id === user.id)
      && conversation.participants.some((participant) => participant.id === this.currentUserId),
    );
  }

  private afterSend(message?: ChatMessage): void {
    this.sending = false;
    if (this.selectedUser?.id === this.sendingTo && this.draft === this.sentDraft) this.draft = '';
    if (this.sendingTo && this.drafts.get(this.sendingTo) === this.sentDraft) this.drafts.delete(this.sendingTo);
    if (message) this.conversations = this.conversations.map(item => item.id === message.conversationId
      ? { ...item, lastMessage: message, updatedAt: message.sentAt } : item);
    if (message && this.selectedUser?.id === this.sendingTo && !this.messages.some((current) => current.id === message.id)) {
      this.messages = [...this.messages, message];
      this.scrollToLatestMessage();
    }
    this.cdr.markForCheck();
  }

  private receiveMessage(message: ChatMessage): void {
    this.conversations = this.conversations.map(item => item.id === message.conversationId
      ? { ...item, lastMessage: message, updatedAt: message.sentAt, unreadCount: item.unreadCount + 1 } : item);
    if (this.selectedConversation?.id === message.conversationId) {
      if (!this.messages.some((current) => current.id === message.id)) {
        this.messages = [...this.messages, message];
        this.scrollToLatestMessage();
      }
      if (!document.hidden) this.markRead(message.conversationId);
    }
    if (this.selectedConversation?.id !== message.conversationId) this.loadConversations(undefined, true);
    this.cdr.markForCheck();
  }

  private updatePresence(update: PresenceUpdate): void {
    const apply = (user: ClinicUser): ClinicUser =>
      user.username === update.username ? { ...user, online: update.online } : user;

    this.users = this.users.map(apply);
    this.conversations = this.conversations.map((conversation) => ({
      ...conversation,
      participants: conversation.participants.map(apply),
    }));
    if (this.selectedUser?.username === update.username) {
      this.selectedUser = { ...this.selectedUser, online: update.online };
    }
    this.cdr.markForCheck();
  }

  private scrollToLatestMessage(): void {
    window.setTimeout(() => {
      const element = this.messageStack?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }

  private sendFailed(error?: { error?: { messages?: string[] } }): void {
    this.sending = false;
    this.error = error?.error?.messages?.join(' ') || 'Message was not confirmed. Your draft is saved; check the conversation before trying again.';
    this.cdr.markForCheck();
  }

  composerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private markRead(id: number): void {
    this.messageService.markRead(id).subscribe({
      next: () => {
        this.conversations = this.conversations.map(item => item.id === id ? { ...item, unreadCount: 0 } : item);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }
}
