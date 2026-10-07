import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { BehaviorSubject, distinctUntilChanged } from 'rxjs';
import { AppLanguage, SOURCE_ALIASES, TRANSLATIONS } from './translations';

const LANGUAGE_STORAGE_KEY = 'dental-clinic-language';
const TRANSLATABLE_ATTRIBUTES = ['placeholder', 'aria-label', 'title'];

@Injectable({ providedIn: 'root' })
export class AppLanguageService implements OnDestroy {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);
  private readonly languageSubject = new BehaviorSubject<AppLanguage>('en');
  private readonly entries = new Map(
    TRANSLATIONS.map((entry) => [this.normalize(entry.en), entry]),
  );
  private readonly aliases = new Map(
    Object.entries(SOURCE_ALIASES).map(([source, target]) => [this.normalize(source), target]),
  );
  private readonly originalText = new WeakMap<Text, string>();
  private readonly renderedText = new WeakMap<Text, string>();
  private readonly originalAttributes = new WeakMap<Element, Map<string, string>>();
  private readonly renderedAttributes = new WeakMap<Element, Map<string, string>>();
  private observer?: MutationObserver;

  readonly language$ = this.languageSubject.asObservable().pipe(distinctUntilChanged());
  readonly languages: { code: AppLanguage; shortLabel: string; label: string }[] = [
    { code: 'ar', shortLabel: 'AR', label: 'العربية' },
    { code: 'en', shortLabel: 'EN', label: 'English' },
    { code: 'fr', shortLabel: 'FR', label: 'Français' },
  ];

  constructor() {
    if (!isPlatformBrowser(this.platformId)) return;
    const saved = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    const language: AppLanguage = saved === 'ar' || saved === 'fr' ? saved : 'en';
    this.languageSubject.next(language);
    this.applyDocumentLanguage(language);
    queueMicrotask(() => this.startDomTranslation());
  }

  get currentLanguage(): AppLanguage {
    return this.languageSubject.value;
  }

  get locale(): string {
    return this.currentLanguage === 'ar'
      ? 'ar-TN'
      : this.currentLanguage === 'fr'
        ? 'fr-FR'
        : 'en-US';
  }

  setLanguage(language: AppLanguage): void {
    if (language === this.currentLanguage) return;
    this.languageSubject.next(language);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
      this.applyDocumentLanguage(language);
      if (this.document.body) this.translateSubtree(this.document.body);
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  translate(value: string, language = this.currentLanguage): string {
    if (!value?.trim()) return value;
    const leading = value.match(/^\s*/)?.[0] ?? '';
    const trailing = value.match(/\s*$/)?.[0] ?? '';
    const source = value.trim().replace(/\s+/g, ' ');
    const canonical = this.aliases.get(this.normalize(source)) ?? source;
    const entry = this.entries.get(this.normalize(canonical));
    if (entry) return `${leading}${entry[language]}${trailing}`;

    const appointments = source.match(/^(\d+)\s+appointments$/i);
    if (appointments) {
      const label = language === 'fr' ? 'rendez-vous' : language === 'ar' ? 'مواعيد' : 'appointments';
      return `${leading}${appointments[1]} ${label}${trailing}`;
    }
    const unread = source.match(/^(\d+)\s+unread notifications$/i);
    if (unread) {
      const label = language === 'fr' ? 'notifications non lues' : language === 'ar' ? 'إشعارات غير مقروءة' : 'unread notifications';
      return `${leading}${unread[1]} ${label}${trailing}`;
    }
    return value;
  }

  private startDomTranslation(): void {
    if (!this.document.body || this.observer) return;
    this.translateSubtree(this.document.body);
    this.observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        if (mutation.type === 'characterData' && mutation.target instanceof Text) {
          this.translateTextNode(mutation.target);
          continue;
        }
        if (mutation.type === 'attributes' && mutation.target instanceof Element) {
          this.translateElementAttributes(mutation.target);
          continue;
        }
        mutation.addedNodes.forEach((node) => this.translateSubtree(node));
      }
    });
    this.observer.observe(this.document.body, {
      subtree: true,
      childList: true,
      characterData: true,
      attributes: true,
      attributeFilter: TRANSLATABLE_ATTRIBUTES,
    });
  }

  private translateSubtree(node: Node): void {
    if (node instanceof Text) {
      this.translateTextNode(node);
      return;
    }
    if (!(node instanceof Element) || this.shouldIgnore(node)) return;
    this.translateElementAttributes(node);
    const walker = this.document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
    let textNode = walker.nextNode();
    while (textNode) {
      this.translateTextNode(textNode as Text);
      textNode = walker.nextNode();
    }
    node.querySelectorAll('*').forEach((element) => {
      if (!this.shouldIgnore(element)) this.translateElementAttributes(element);
    });
  }

  private translateTextNode(node: Text): void {
    const parent = node.parentElement;
    if (!parent || this.shouldIgnore(parent)) return;
    const current = node.nodeValue ?? '';
    const lastRendered = this.renderedText.get(node);
    if (!this.originalText.has(node) || current !== lastRendered) this.originalText.set(node, current);
    const source = this.originalText.get(node) ?? current;
    const translated = this.translate(source);
    this.renderedText.set(node, translated);
    if (current !== translated) node.nodeValue = translated;
  }

  private translateElementAttributes(element: Element): void {
    let originals = this.originalAttributes.get(element);
    let rendered = this.renderedAttributes.get(element);
    if (!originals) {
      originals = new Map<string, string>();
      this.originalAttributes.set(element, originals);
    }
    if (!rendered) {
      rendered = new Map<string, string>();
      this.renderedAttributes.set(element, rendered);
    }
    for (const attribute of TRANSLATABLE_ATTRIBUTES) {
      if (!element.hasAttribute(attribute)) continue;
      const current = element.getAttribute(attribute) ?? '';
      if (!originals.has(attribute) || current !== rendered.get(attribute)) originals.set(attribute, current);
      const translated = this.translate(originals.get(attribute) ?? current);
      rendered.set(attribute, translated);
      if (current !== translated) element.setAttribute(attribute, translated);
    }
  }

  private applyDocumentLanguage(language: AppLanguage): void {
    this.document.documentElement.lang = language;
    this.document.documentElement.dir = language === 'ar' ? 'rtl' : 'ltr';
    this.document.body?.classList.toggle('rtl-layout', language === 'ar');
  }

  private shouldIgnore(element: Element): boolean {
    return !!element.closest('[data-no-translate], script, style, code, [contenteditable="true"]');
  }

  private normalize(value: string): string {
    return value.trim().replace(/\s+/g, ' ').toLocaleLowerCase('en-US');
  }
}
