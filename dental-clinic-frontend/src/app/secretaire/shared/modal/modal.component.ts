import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ClinicIconComponent } from '../clinic-icon/clinic-icon.component';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule, ClinicIconComponent],
  template: `
    <div class="modal-backdrop" *ngIf="open" (click)="closeModal()">
      <section class="modal-panel" role="dialog" aria-modal="true" [attr.aria-label]="title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <div>
            <p class="eyebrow">{{ eyebrow }}</p>
            <h2>{{ title }}</h2>
          </div>
          <button class="icon-button" type="button" (click)="closeModal()" aria-label="Close dialog">
            <app-clinic-icon name="close"></app-clinic-icon>
          </button>
        </header>
        <ng-content />
      </section>
    </div>
  `,
  styleUrl: './modal.component.css',
})
export class ModalComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() eyebrow = 'Clinic workspace';
  @Output() dismissed = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  closeModal(): void {
    this.dismissed.emit();
    this.close.emit();
  }
}
