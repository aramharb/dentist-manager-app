import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { Doctor } from './doctor';

describe('Doctor', () => {
  let component: Doctor;
  let fixture: ComponentFixture<Doctor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Doctor],
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Doctor);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
