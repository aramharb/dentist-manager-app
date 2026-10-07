import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppLanguageService } from './app-language.service';

describe('AppLanguageService', () => {
  beforeEach(() => {
    localStorage.removeItem('dental-clinic-language');
    document.documentElement.lang = 'en';
    document.documentElement.dir = 'ltr';
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
  });

  afterEach(() => {
    localStorage.removeItem('dental-clinic-language');
    document.documentElement.lang = 'en';
    document.documentElement.dir = 'ltr';
    TestBed.resetTestingModule();
  });

  it('translates interface text in French and Arabic', () => {
    const service = TestBed.inject(AppLanguageService);

    expect(service.translate('Appointments', 'fr')).toBe('Rendez-vous');
    expect(service.translate('Appointments', 'ar')).toBe('المواعيد');
    expect(service.translate('Assigned doctor', 'ar')).toBe('الطبيب المسؤول');
  });

  it('persists the language and enables RTL for Arabic', () => {
    const service = TestBed.inject(AppLanguageService);

    service.setLanguage('ar');

    expect(service.currentLanguage).toBe('ar');
    expect(localStorage.getItem('dental-clinic-language')).toBe('ar');
    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('returns to left-to-right layout for English and French', () => {
    const service = TestBed.inject(AppLanguageService);
    service.setLanguage('ar');

    service.setLanguage('fr');

    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
  });
});
