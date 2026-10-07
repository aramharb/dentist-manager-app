import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { SessionService } from '../../../core/auth/session.service';
import { MessageService } from '../../../shared/services/message.service';
import { Appointment, AppointmentView, DoctorWorkingHours } from '../../models/secretary.models';
import { AppointmentService } from '../../services/appointment.service';
import { PatientService } from '../../services/patient.service';
import { AppointmentsComponent } from './appointments.component';

describe('AppointmentsComponent', () => {
  let component: AppointmentsComponent;
  let appointmentService: jasmine.SpyObj<AppointmentService> & { refreshSignal: Subject<void> };

  beforeEach(async () => {
    const refreshSignal = new Subject<void>();
    appointmentService = jasmine.createSpyObj<AppointmentService>('AppointmentService', [
      'getAppointments', 'getDoctors', 'getWorkingHours', 'updateWorkingHours',
      'createAppointment', 'updateAppointment', 'cancelAppointment', 'deleteAppointment',
    ]) as jasmine.SpyObj<AppointmentService> & { refreshSignal: Subject<void> };
    appointmentService.refreshSignal = refreshSignal;
    Object.defineProperty(appointmentService, 'refreshRequested$', { value: refreshSignal.asObservable() });
    appointmentService.getAppointments.and.returnValue(of([]));
    appointmentService.getDoctors.and.returnValue(of([]));

    const patientService = jasmine.createSpyObj<PatientService>('PatientService', ['getPatients']);
    patientService.getPatients.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AppointmentsComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: AppointmentService, useValue: appointmentService },
        { provide: PatientService, useValue: patientService },
        { provide: MessageService, useValue: { scheduleEvents$: new Subject() } },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        { provide: SessionService, useValue: { currentUser: { role: 'doctor' } } },
      ],
    }).compileComponents();

    component = TestBed.createComponent(AppointmentsComponent).componentInstance;
  });

  it('loads the unfiltered database list on entry and repeated refresh signals', () => {
    component.ngOnInit();
    appointmentService.refreshSignal.next();

    expect(appointmentService.getAppointments).toHaveBeenCalledTimes(2);
    expect(appointmentService.getAppointments.calls.allArgs()).toEqual([[], []]);
  });

  it('maps a 30-minute drop slot to the selected doctor and preserves duration', () => {
    component.selectedDate = '2030-01-07';
    component.workingHours.set(2, weekdayHours(2));
    const original = appointmentView({ id: 5, startTime: '09:00', endTime: '10:00', durationMinutes: 60 });
    const updated = appointment({ id: 5, startTime: '11:30', endTime: '12:30', durationMinutes: 60 });
    appointmentService.updateAppointment.and.returnValue(of(updated));

    component.dragStarted(original);
    component.dropAppointment({ preventDefault: () => undefined } as DragEvent, 2, '11:30');

    expect(appointmentService.updateAppointment).toHaveBeenCalledWith(5, jasmine.objectContaining({
      date: '2030-01-07', providerUserId: 2, startTime: '11:30', endTime: '12:30',
    }));
    expect(component.appointments[0]).toEqual(updated);
  });

  it('uses minute duration for proportional block height', () => {
    const style = component.appointmentStyle(appointmentView({ durationMinutes: 90 }));

    expect(style['height']).toBe('135px');
  });

  it('keeps the original appointment when the backend rejects a drop', () => {
    component.selectedDate = '2030-01-07';
    component.workingHours.set(2, weekdayHours(2));
    const raw = appointment({ id: 8, startTime: '09:00', endTime: '10:00', durationMinutes: 60 });
    component.appointments = [raw];
    appointmentService.updateAppointment.and.returnValue(throwError(() => ({
      error: { messages: ['This appointment overlaps with another appointment or is less than 30 minutes away.'] },
    })));

    component.dragStarted(component.appointmentViews[0]);
    component.dropAppointment({ preventDefault: () => undefined } as DragEvent, 2, '11:30');

    expect(component.appointments).toEqual([raw]);
    expect(component.error).toContain('overlaps');
  });

  it('moves a cancelled appointment out of active lanes and into the cancelled section', () => {
    component.selectedDate = '2030-01-07';
    const raw = appointment({ id: 11 });
    component.appointments = [raw];
    appointmentService.cancelAppointment.and.returnValue(of({ ...raw, status: 'CANCELLED' }));

    component.requestCancellation(component.appointmentViews[0]);
    component.confirmCancellation();

    expect(component.filteredSelectedAppointments).toEqual([]);
    expect(component.cancelledAppointments.map((item) => item.id)).toEqual([11]);
  });

  it('flags preserved legacy rows with no provider assignment', () => {
    component.appointments = [appointment({ providerUserId: null, providerName: 'Legacy Dentist' })];

    expect(component.appointmentViews[0].warnings).toContain('Unassigned provider');
  });

  function weekdayHours(doctorUserId: number): DoctorWorkingHours {
    return {
      doctorUserId,
      doctorName: 'Dr. Test',
      days: Array.from({ length: 7 }, (_, index) => ({
        dayOfWeek: index + 1,
        working: index < 5,
        startTime: index < 5 ? '08:00' : null,
        endTime: index < 5 ? '17:00' : null,
      })),
    };
  }

  function appointment(overrides: Partial<Appointment> = {}): Appointment {
    return {
      id: 1,
      patientId: 4,
      patientFirstName: 'Test',
      patientLastName: 'Patient',
      date: '2030-01-07',
      startTime: '10:00',
      endTime: '10:30',
      durationMinutes: 30,
      providerName: 'Dr. Test',
      providerUserId: 2,
      priority: 'NORMAL',
      status: 'SCHEDULED',
      clientId: 4,
      clientName: 'Test Patient',
      time: '10:00',
      duration: 30,
      treatmentType: 'General visit',
      dentist: 'Dr. Test',
      color: '#1689e8',
      ...overrides,
    };
  }

  function appointmentView(overrides: Partial<AppointmentView> = {}): AppointmentView {
    return {
      ...appointment(overrides),
      patientId: overrides.patientId ?? 4,
      heure: overrides.heure ?? overrides.startTime ?? '10:00',
      startTime: overrides.startTime ?? '10:00',
      endTime: overrides.endTime ?? '10:30',
      durationMinutes: overrides.durationMinutes ?? 30,
      providerName: overrides.providerName ?? 'Dr. Test',
      priority: overrides.priority ?? 'NORMAL',
      status: overrides.status ?? 'SCHEDULED',
      warnings: overrides.warnings ?? [],
    } as AppointmentView;
  }
});
