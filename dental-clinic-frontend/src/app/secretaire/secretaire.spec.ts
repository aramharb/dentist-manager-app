import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { Secretaire } from './secretaire';

describe('Secretaire', () => {
  let component: Secretaire;
  let fixture: ComponentFixture<Secretaire>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Secretaire],
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Secretaire);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
