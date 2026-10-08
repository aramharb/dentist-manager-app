import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppLanguageService } from './core/i18n/app-language.service';
import { CabinetThemeService } from './core/theme/cabinet-theme.service';
import { NotificationContainerComponent } from './core/notifications/notification-container.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NotificationContainerComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly language = inject(AppLanguageService);
  private readonly theme = inject(CabinetThemeService);

  constructor() {
    this.theme.start();
  }
  protected readonly title = signal('dental-clinic-frontend');
}
