import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ClinicIconComponent } from '../clinic-icon/clinic-icon.component';

@Component({
  selector: 'app-secretary-nav-item',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, ClinicIconComponent],
  template: `
    <a
      class="nav-item"
      [routerLink]="link"
      routerLinkActive="active"
      [routerLinkActiveOptions]="{ exact: true }"
      (click)="navigate.emit(link)"
    >
      <app-clinic-icon class="nav-icon" [name]="icon"></app-clinic-icon>
      <span>{{ label }}</span>
    </a>
  `,
  styleUrl: './nav-item.component.css',
})
export class NavItemComponent {
  @Input() icon = '';
  @Input() label = '';
  @Input() link = '';
  @Output() navigate = new EventEmitter<string>();
}
