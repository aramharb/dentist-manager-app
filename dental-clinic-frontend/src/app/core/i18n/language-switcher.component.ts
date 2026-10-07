import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AppLanguageService } from './app-language.service';
import { AppLanguage } from './translations';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="language-switcher" role="group" aria-label="Interface language" data-no-translate>
      <button
        *ngFor="let language of languageService.languages"
        type="button"
        [class.active]="language.code === languageService.currentLanguage"
        [attr.aria-pressed]="language.code === languageService.currentLanguage"
        [attr.aria-label]="language.label"
        [title]="language.label"
        (click)="select(language.code)">
        {{ language.shortLabel }}
      </button>
    </div>
  `,
  styles: [`
    .language-switcher {
      direction: ltr;
      display: inline-grid;
      grid-template-columns: repeat(3, 34px);
      align-items: center;
      gap: 3px;
      min-height: 44px;
      padding: 4px;
      border: 1px solid rgba(205, 224, 238, .9);
      border-radius: 15px;
      background: rgba(237, 246, 253, .95);
    }
    button {
      min-width: 34px;
      height: 34px;
      padding: 0 5px;
      border: 0;
      border-radius: 11px;
      color: #5f7284;
      background: transparent;
      font: 800 11px/1 Inter, sans-serif;
      transition: color .2s ease, background .2s ease, box-shadow .2s ease, transform .2s ease;
    }
    button:hover { color: #0f67c0; transform: translateY(-1px); }
    button.active {
      color: white;
      background: linear-gradient(135deg, #1689e8, #06b6d4);
      box-shadow: 0 6px 14px rgba(22, 137, 232, .24);
    }
    :host-context(.dark-mode) .language-switcher {
      border-color: rgba(125, 169, 201, .25);
      background: rgba(24, 54, 76, .94);
    }
    :host-context(.dark-mode) button { color: #b9d2e5; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSwitcherComponent {
  readonly languageService = inject(AppLanguageService);

  select(language: AppLanguage): void {
    this.languageService.setLanguage(language);
  }
}
