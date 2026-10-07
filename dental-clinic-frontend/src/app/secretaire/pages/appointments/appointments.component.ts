import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { ClinicUser, MessageService, ScheduleEvent } from '../../../shared/services/message.service';
import {
  Appointment,
  AppointmentPayload,
  AppointmentView,
  DoctorWorkingDay,
  DoctorWorkingHours,
  Patient,
} from '../../models/secretary.models';
import { AppointmentService } from '../../services/appointment.service';
import { PatientService } from '../../services/patient.service';
import { ModalComponent } from '../../shared/modal/modal.component';
import { ActivatedRoute } from '@angular/router';

type CalendarState = 'available' | 'unavailable' | 'partial' | 'full';
type SlotState = 'available' | 'occupied' | 'unavailable';

interface CalendarDayCell {
  date: Date;
  iso: string;
  otherMonth: boolean;
  weekend: boolean;
  past: boolean;
  state: CalendarState;
  appointmentCount: number;
  availableDoctorCount: number;
  doctorIndicators: DoctorAvailabilityIndicator[];
}

interface DoctorAvailabilityIndicator {
  doctorId: number;
  doctorName: string;
  initials: string;
  working: boolean;
  hoursLabel: string;
  appointmentCount: number;
  bookable: boolean;
}

@Component({
  selector: 'app-secretary-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.css',
})
export class AppointmentsComponent implements OnInit, OnDestroy {
  private readonly appointmentService = inject(AppointmentService);
  private readonly messageService = inject(MessageService);
  private readonly patientService = inject(PatientService);
  private readonly sessionService = inject(SessionService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly subscriptions = new Subscription();
  private readonly pixelsPerMinute = 1.5;

  patients: Patient[] = [];
  doctors: ClinicUser[] = [];
  appointments: Appointment[] = [];
  workingHours = new Map<number, DoctorWorkingHours>();
  selectedDate = '';
  selectedDoctorId = 0;
  readonly today = this.formatDateKey(new Date());
  viewDate = new Date();
  appointmentModal = false;
  editingAppointmentId?: number;
  pendingCancellation?: AppointmentView;
  draggingAppointment?: AppointmentView;
  hoursDoctorId = 0;
  editableWorkingDays: DoctorWorkingDay[] = [];
  savingHours = false;
  dentistFilter = '';
  statusFilter: AppointmentPayload['status'] | '' = '';
  query = '';
  loading = false;
  saving = false;
  error = '';
  success = '';
  readonly statuses: AppointmentPayload['status'][] = ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  readonly editableStatuses: AppointmentPayload['status'][] = ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED'];
  readonly priorities: AppointmentPayload['priority'][] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];
  readonly weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  readonly weekDayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  form: AppointmentPayload = this.emptyForm();
  requestedPatientId = 0;
  requestedPatientName = '';

  get isDoctor(): boolean {
    return this.sessionService.currentUser?.role === 'doctor';
  }

  get canEditWorkingHours(): boolean {
    const user = this.sessionService.currentUser;
    return user?.role === 'doctor' && user.id === this.hoursDoctorId;
  }

  get hoursDoctors(): ClinicUser[] {
    const user = this.sessionService.currentUser;
    return this.isDoctor ? this.doctors.filter((doctor) => doctor.id === user?.id) : this.doctors;
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.requestedPatientId = Number(this.route.snapshot.queryParamMap.get('patientId')) || 0;
    this.requestedPatientName = this.route.snapshot.queryParamMap.get('patientName')?.trim() ?? '';
    this.loadPatients();
    this.loadDoctorsAndHours();
    this.loadAppointments();
    this.subscriptions.add(this.appointmentService.refreshRequested$.subscribe(() => this.loadAppointments()));
    this.subscriptions.add(this.messageService.scheduleEvents$.subscribe((event) => this.handleScheduleEvent(event)));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  get monthLabel(): string {
    return this.viewDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  get requestedPatientDisplayName(): string {
    if (this.requestedPatientName) return this.requestedPatientName;
    const patient = this.patients.find((item) => item.id === this.requestedPatientId);
    return patient ? `${patient.firstName} ${patient.lastName}` : '';
  }

  get selectedDateLabel(): string {
    if (!this.selectedDate) return '';
    return this.parseDateKey(this.selectedDate).toLocaleDateString('en-US', {
      weekday: 'long', month: 'long', day: 'numeric', year: 'numeric',
    });
  }

  get appointmentViews(): AppointmentView[] {
    const views = this.appointments.map((appointment) => this.toView(appointment));
    return views.map((view) => ({ ...view, warnings: this.warningsFor(view, views) }));
  }

  get selectedAppointments(): AppointmentView[] {
    if (!this.selectedDate) return [];
    return this.appointmentViews
      .filter((appointment) => appointment.date === this.selectedDate)
      .sort((left, right) => left.startTime.localeCompare(right.startTime));
  }

  get filteredSelectedAppointments(): AppointmentView[] {
    return this.selectedAppointments
      .filter((appointment) => appointment.status !== 'CANCELLED')
      .filter((appointment) => !this.dentistFilter || String(appointment.providerUserId) === this.dentistFilter)
      .filter((appointment) => !this.statusFilter || appointment.status === this.statusFilter);
  }

  get selectedTimelineAppointments(): AppointmentView[] {
    return this.selectedAppointments
      .filter((appointment) => appointment.status !== 'CANCELLED')
      .filter((appointment) => appointment.providerUserId === this.selectedDoctorId)
      .filter((appointment) => !this.statusFilter || appointment.status === this.statusFilter);
  }

  get cancelledAppointments(): AppointmentView[] {
    return this.selectedAppointments.filter((appointment) => appointment.status === 'CANCELLED'
      && (!this.selectedDoctorId || appointment.providerUserId === this.selectedDoctorId));
  }

  get legacySelectedAppointments(): AppointmentView[] {
    return this.selectedAppointments.filter((appointment) => appointment.status !== 'CANCELLED' && !appointment.providerUserId);
  }

  get tableAppointments(): AppointmentView[] {
    const value = this.query.trim().toLowerCase();
    return this.appointmentViews
      .filter((appointment) => !value
        || `${appointment.clientName} ${appointment.providerName} ${appointment.treatmentType}`.toLowerCase().includes(value))
      .filter((appointment) => !this.dentistFilter || String(appointment.providerUserId) === this.dentistFilter)
      .filter((appointment) => !this.statusFilter || appointment.status === this.statusFilter)
      .sort((left, right) => `${left.date} ${left.startTime}`.localeCompare(`${right.date} ${right.startTime}`));
  }

  get availableDoctors(): ClinicUser[] {
    return this.selectedDate ? this.availableDoctorsForDate(this.selectedDate) : [];
  }

  get selectedDoctor(): ClinicUser | undefined {
    return this.availableDoctors.find((doctor) => doctor.id === this.selectedDoctorId);
  }

  get selectedWorkingDay(): DoctorWorkingDay | undefined {
    return this.selectedDate && this.selectedDoctorId
      ? this.workingDay(this.selectedDoctorId, this.selectedDate)
      : undefined;
  }

  get timelineStartMinutes(): number {
    return this.timeToMinutes(this.selectedWorkingDay?.startTime);
  }

  get timelineEndMinutes(): number {
    return this.timeToMinutes(this.selectedWorkingDay?.endTime);
  }

  get timeSlots(): string[] {
    const start = this.timelineStartMinutes;
    const end = this.timelineEndMinutes;
    if (end <= start) return [];
    const slots: string[] = [];
    for (let minute = start; minute < end; minute += 30) slots.push(this.minutesToTime(minute));
    return slots;
  }

  get timelineHeight(): number {
    return Math.max(0, this.timelineEndMinutes - this.timelineStartMinutes) * this.pixelsPerMinute;
  }

  get isSelectedDatePast(): boolean {
    return !!this.selectedDate && this.selectedDate < this.today;
  }

  get canCreateOnSelectedDate(): boolean {
    return !!this.selectedDate && !this.isSelectedDatePast && !!this.selectedDoctorId
      && this.availableDoctors.some((doctor) => doctor.id === this.selectedDoctorId)
      && this.timeSlots.some((slot) => this.slotState(slot) === 'available');
  }

  get formDuration(): number {
    return Math.max(0, this.timeToMinutes(this.form.endTime) - this.timeToMinutes(this.form.startTime));
  }

  get formAvailableDoctors(): ClinicUser[] {
    return this.form.date ? this.availableDoctorsForDate(this.form.date) : [];
  }

  get formDoctorOptions(): ClinicUser[] {
    const available = this.formAvailableDoctors;
    if (!this.editingAppointmentId || !this.form.providerUserId
      || available.some((doctor) => doctor.id === this.form.providerUserId)) return available;
    const assigned = this.doctors.find((doctor) => doctor.id === this.form.providerUserId);
    return assigned ? [assigned, ...available] : available;
  }

  get selectedDateDoctorAvailability(): DoctorAvailabilityIndicator[] {
    if (!this.selectedDate) return [];
    return this.doctorIndicatorsForDate(this.selectedDate);
  }

  get calendarDays(): CalendarDayCell[] {
    const start = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth(), 1);
    const offset = (start.getDay() + 6) % 7;
    const first = new Date(start);
    first.setDate(start.getDate() - offset);
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(first);
      date.setDate(first.getDate() + index);
      const iso = this.formatDateKey(date);
      const appointmentCount = this.activeAppointmentsForDate(iso).length;
      const availableDoctors = this.availableDoctorsForDate(iso);
      const doctorIndicators = this.doctorIndicatorsForDate(iso);
      const hasOpenTime = availableDoctors.some((doctor) => this.doctorHasBookableTime(doctor.id, iso));
      const state: CalendarState = availableDoctors.length === 0
        ? 'unavailable'
        : iso < this.today
          ? appointmentCount > 0 ? 'partial' : 'available'
        : !hasOpenTime
          ? 'full'
          : appointmentCount > 0 ? 'partial' : 'available';
      return {
        date,
        iso,
        otherMonth: date.getMonth() !== this.viewDate.getMonth(),
        weekend: [0, 6].includes(date.getDay()),
        past: iso < this.today,
        state,
        appointmentCount,
        availableDoctorCount: availableDoctors.length,
        doctorIndicators,
      };
    });
  }

  changeMonth(direction: number): void {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + direction, 1);
  }

  selectDate(date: string): void {
    this.selectedDate = date;
    this.error = '';
    this.success = '';
    const available = this.availableDoctorsForDate(date);
    const ownDoctor = this.sessionService.currentUser?.id;
    const preferred = available.find((doctor) => doctor.id === this.selectedDoctorId)
      ?? available.find((doctor) => doctor.id === ownDoctor)
      ?? available[0];
    this.selectedDoctorId = preferred?.id ?? 0;
    this.form.date = date;
    this.form.providerUserId = this.selectedDoctorId;
    this.cdr.markForCheck();
  }

  selectScheduleDoctor(doctorId: number): void {
    this.selectedDoctorId = Number(doctorId);
    this.form.providerUserId = this.selectedDoctorId;
  }

  loadAppointments(silent = false): void {
    if (!silent) {
      this.loading = true;
      this.error = '';
    }
    this.appointmentService.getAppointments().subscribe({
      next: (appointments) => {
        this.appointments = appointments;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'Unable to load appointments.');
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadPatients(): void {
    this.patientService.getPatients().subscribe({
      next: (patients) => { this.patients = patients; this.cdr.markForCheck(); },
      error: () => { this.error = 'Unable to load patients.'; this.cdr.markForCheck(); },
    });
  }

  loadDoctorsAndHours(): void {
    this.appointmentService.getDoctors().subscribe({
      next: (users) => {
        const activeDoctors = users.filter((user) => user.role === 'doctor');
        const ownUserId = this.sessionService.currentUser?.id;
        this.doctors = this.isDoctor
          ? activeDoctors.filter((doctor) => doctor.id === ownUserId)
          : activeDoctors;
        const preferredHoursDoctor = this.isDoctor
          ? this.doctors.find((doctor) => doctor.id === this.sessionService.currentUser?.id)
          : this.doctors[0];
        this.hoursDoctorId = preferredHoursDoctor?.id ?? 0;
        const requests = this.doctors.map((doctor) => this.appointmentService.getWorkingHours(doctor.id));
        if (!requests.length) {
          this.editableWorkingDays = [];
          this.cdr.markForCheck();
          return;
        }
        forkJoin(requests).subscribe({
          next: (responses) => {
            this.workingHours.clear();
            responses.forEach((response) => this.workingHours.set(response.doctorUserId, response));
            this.selectHoursDoctor(this.hoursDoctorId);
            if (this.selectedDate) this.selectDate(this.selectedDate);
            this.cdr.markForCheck();
          },
          error: () => { this.error = 'Unable to load doctor working hours.'; this.cdr.markForCheck(); },
        });
      },
      error: () => { this.error = 'Unable to load active doctors.'; this.cdr.markForCheck(); },
    });
  }

  eventsByDate(date: string): AppointmentView[] {
    return this.activeAppointmentsForDate(date);
  }

  availableDoctorsForDate(date: string): ClinicUser[] {
    if (!date) return [];
    return this.doctors.filter((doctor) => {
      const hours = this.workingDay(doctor.id, date);
      return !!hours?.working && !!hours.startTime && !!hours.endTime;
    });
  }

  calendarStateLabel(state: CalendarState): string {
    const labels: Record<CalendarState, string> = {
      available: 'Available', unavailable: 'Unavailable', partial: 'Partially booked', full: 'Fully booked',
    };
    return labels[state];
  }

  slotState(startTime: string): SlotState {
    if (!this.selectedDate || !this.selectedDoctorId || this.isSelectedDatePast) return 'unavailable';
    const start = this.timeToMinutes(startTime);
    const end = start + 30;
    if (start < this.timelineStartMinutes || end > this.timelineEndMinutes) return 'unavailable';
    const appointments = this.activeAppointmentsForDoctor(this.selectedDoctorId, this.selectedDate);
    const occupied = appointments.some((appointment) => start < this.timeToMinutes(appointment.endTime)
      && end > this.timeToMinutes(appointment.startTime));
    if (occupied) return 'occupied';
    const blockedBySpacing = appointments.some((appointment) => start < this.timeToMinutes(appointment.endTime) + 30
      && end > this.timeToMinutes(appointment.startTime) - 30);
    return blockedBySpacing ? 'unavailable' : 'available';
  }

  appointmentStyle(appointment: AppointmentView): Record<string, string> {
    const top = (this.timeToMinutes(appointment.startTime) - this.timelineStartMinutes) * this.pixelsPerMinute;
    const height = Math.max(30, appointment.durationMinutes * this.pixelsPerMinute);
    return { top: `${top}px`, height: `${height}px`, '--accent': appointment.color };
  }

  openSlot(startTime: string): void {
    if (this.slotState(startTime) !== 'available') return;
    this.openAppointmentModal(undefined, startTime);
  }

  openAppointmentModal(appointment?: AppointmentView, suggestedStart?: string): void {
    this.error = '';
    this.success = '';
    if (!appointment) {
      if (!this.selectedDate) {
        this.error = 'Choose a date before creating an appointment.';
        return;
      }
      if (this.isSelectedDatePast) {
        this.error = 'New appointments cannot be scheduled in the past.';
        return;
      }
      if (!this.selectedDoctorId) {
        this.error = 'No doctor is available on this date.';
        return;
      }
      const start = suggestedStart ?? this.timeSlots.find((slot) => this.slotState(slot) === 'available');
      if (!start) {
        this.error = 'No available appointment time remains for this doctor on the selected date.';
        return;
      }
      this.editingAppointmentId = undefined;
      this.form = {
        ...this.emptyForm(),
        patientId: this.requestedPatientId,
        date: this.selectedDate,
        providerUserId: this.selectedDoctorId,
        startTime: start,
        endTime: this.addMinutes(start, 30),
      };
    } else {
      this.editingAppointmentId = appointment.id;
      this.form = {
        patientId: appointment.patientId,
        treatmentId: appointment.treatmentId,
        date: appointment.date,
        startTime: appointment.startTime.slice(0, 5),
        endTime: appointment.endTime.slice(0, 5),
        providerUserId: appointment.providerUserId ?? 0,
        priority: this.toApiPriority(appointment.priority),
        status: this.toApiStatus(appointment.status) === 'CANCELLED' ? 'SCHEDULED' : this.toApiStatus(appointment.status),
        notes: appointment.notes ?? '',
      };
    }
    this.appointmentModal = true;
  }

  formDateChanged(): void {
    if (this.isDoctor) {
      this.form.providerUserId = this.sessionService.currentUser?.id ?? 0;
      return;
    }
    if (!this.formAvailableDoctors.some((doctor) => doctor.id === this.form.providerUserId)) {
      this.form.providerUserId = this.formAvailableDoctors[0]?.id ?? 0;
    }
  }

  startChanged(): void {
    if (!this.editingAppointmentId) this.form.endTime = this.addMinutes(this.form.startTime, 30);
  }

  saveAppointment(): void {
    const validation = this.validateLocally(this.form, this.editingAppointmentId);
    if (validation) {
      this.error = validation;
      return;
    }
    this.saving = true;
    const wasEditing = !!this.editingAppointmentId;
    const request = this.editingAppointmentId
      ? this.appointmentService.updateAppointment(this.editingAppointmentId, this.form)
      : this.appointmentService.createAppointment(this.form);
    request.subscribe({
      next: (appointment) => {
        this.upsertAppointment(appointment);
        this.appointmentModal = false;
        this.saving = false;
        this.success = wasEditing ? 'Appointment updated.' : 'Appointment created.';
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'Unable to save appointment.');
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  requestCancellation(appointment: AppointmentView): void {
    this.pendingCancellation = appointment;
  }

  confirmCancellation(): void {
    const appointment = this.pendingCancellation;
    if (!appointment) return;
    this.appointmentService.cancelAppointment(appointment.id).subscribe({
      next: (updated) => {
        this.upsertAppointment(updated);
        this.pendingCancellation = undefined;
        this.success = 'Appointment cancelled.';
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'Unable to cancel appointment.');
        this.pendingCancellation = undefined;
        this.cdr.markForCheck();
      },
    });
  }

  deleteCancelledAppointment(appointment: AppointmentView): void {
    if (appointment.status !== 'CANCELLED') return;
    if (!window.confirm(`Delete the cancelled appointment for ${appointment.clientName}? This cannot be undone.`)) return;
    this.appointmentService.deleteAppointment(appointment.id).subscribe({
      next: () => {
        this.appointments = this.appointments.filter((item) => item.id !== appointment.id);
        this.success = 'Cancelled appointment deleted.';
        this.loadAppointments(true);
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'Unable to delete cancelled appointment.');
        this.cdr.markForCheck();
      },
    });
  }

  dragStarted(appointment: AppointmentView): void {
    if (this.isSelectedDatePast) {
      this.error = 'Past appointments cannot be rescheduled.';
      return;
    }
    this.draggingAppointment = appointment;
    this.error = '';
  }

  dragEnded(): void {
    this.draggingAppointment = undefined;
  }

  allowDrop(event: DragEvent, slot: string): void {
    if (this.draggingAppointment && this.slotStateForAppointment(slot, this.draggingAppointment) === 'available') {
      event.preventDefault();
    }
  }

  dropAppointment(event: DragEvent, doctorId: number, startTime: string): void {
    event.preventDefault();
    const appointment = this.draggingAppointment;
    this.draggingAppointment = undefined;
    if (!appointment || !this.selectedDate || this.isSelectedDatePast) return;
    const payload: AppointmentPayload = {
      patientId: appointment.patientId,
      treatmentId: appointment.treatmentId,
      date: this.selectedDate,
      startTime,
      endTime: this.addMinutes(startTime, appointment.durationMinutes),
      providerUserId: doctorId,
      priority: appointment.priority,
      status: appointment.status,
      notes: appointment.notes,
    };
    const validation = this.validateLocally(payload, appointment.id);
    if (validation) {
      this.error = validation;
      return;
    }
    this.appointmentService.updateAppointment(appointment.id, payload).subscribe({
      next: (updated) => {
        this.upsertAppointment(updated);
        this.success = `Appointment moved to ${startTime}.`;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'The appointment could not be moved and remains in its previous slot.');
        this.cdr.markForCheck();
      },
    });
  }

  selectHoursDoctor(doctorId: number): void {
    const ownDoctorId = this.sessionService.currentUser?.id;
    this.hoursDoctorId = this.isDoctor ? (ownDoctorId ?? 0) : Number(doctorId);
    const schedule = this.workingHours.get(this.hoursDoctorId);
    this.editableWorkingDays = schedule?.days.map((day) => ({ ...day })) ?? [];
  }

  toggleWorkingDay(day: DoctorWorkingDay): void {
    if (!this.canEditWorkingHours) return;
    if (day.working) {
      day.startTime ||= '08:00';
      day.endTime ||= '17:00';
    } else {
      day.startTime = null;
      day.endTime = null;
    }
  }

  saveWorkingHours(): void {
    if (!this.canEditWorkingHours || !this.hoursDoctorId || this.editableWorkingDays.length !== 7) return;
    this.savingHours = true;
    this.appointmentService.updateWorkingHours(this.hoursDoctorId, this.editableWorkingDays).subscribe({
      next: (response) => {
        this.workingHours.set(response.doctorUserId, response);
        this.editableWorkingDays = response.days.map((day) => ({ ...day }));
        this.savingHours = false;
        this.success = 'Your working hours were saved.';
        if (this.selectedDate) this.selectDate(this.selectedDate);
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.apiError(error, 'Unable to save working hours.');
        this.savingHours = false;
        this.cdr.markForCheck();
      },
    });
  }

  statusLabel(status: string): string {
    return status.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
  }

  formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const remainder = minutes % 60;
    if (!hours) return `${remainder} minutes`;
    if (!remainder) return `${hours} ${hours === 1 ? 'hour' : 'hours'}`;
    return `${hours}h ${remainder}m`;
  }

  private validateLocally(payload: AppointmentPayload, excludedId?: number): string {
    const start = this.timeToMinutes(payload.startTime);
    const end = this.timeToMinutes(payload.endTime);
    if (!payload.patientId || !payload.providerUserId) return 'Select a patient and an available doctor.';
    if (!payload.date) return 'Select an appointment date.';
    if (payload.date < this.today) return 'Appointments cannot be scheduled in the past.';
    if (end <= start) return 'Appointment start time must be before end time on the same day.';
    const hours = this.workingDay(payload.providerUserId, payload.date);
    if (!hours?.working || !hours.startTime || !hours.endTime
      || start < this.timeToMinutes(hours.startTime) || end > this.timeToMinutes(hours.endTime)) {
      return "The appointment must fit inside the doctor's working hours.";
    }
    const conflict = this.appointmentViews.some((appointment) => appointment.id !== excludedId
      && appointment.status !== 'CANCELLED'
      && appointment.providerUserId === payload.providerUserId
      && appointment.date === payload.date
      && start < this.timeToMinutes(appointment.endTime) + 30
      && end > this.timeToMinutes(appointment.startTime) - 30);
    return conflict ? 'This appointment overlaps with another appointment or is less than 30 minutes away.' : '';
  }

  private activeAppointmentsForDate(date: string): AppointmentView[] {
    return this.appointmentViews.filter((appointment) => appointment.date === date && appointment.status !== 'CANCELLED');
  }

  private doctorIndicatorsForDate(date: string): DoctorAvailabilityIndicator[] {
    return this.doctors.map((doctor) => {
      const hours = this.workingDay(doctor.id, date);
      const working = !!hours?.working && !!hours.startTime && !!hours.endTime;
      const appointmentCount = this.activeAppointmentsForDoctor(doctor.id, date).length;
      return {
        doctorId: doctor.id,
        doctorName: doctor.fullName,
        initials: this.doctorInitials(doctor.fullName),
        working,
        hoursLabel: working ? `${hours!.startTime!.slice(0, 5)}–${hours!.endTime!.slice(0, 5)}` : 'Not working',
        appointmentCount,
        bookable: working && this.doctorHasBookableTime(doctor.id, date),
      };
    });
  }

  private activeAppointmentsForDoctor(doctorId: number, date: string): AppointmentView[] {
    return this.activeAppointmentsForDate(date).filter((appointment) => appointment.providerUserId === doctorId);
  }

  private doctorHasBookableTime(doctorId: number, date: string): boolean {
    if (date < this.today) return false;
    const hours = this.workingDay(doctorId, date);
    if (!hours?.working || !hours.startTime || !hours.endTime) return false;
    const dayStart = this.timeToMinutes(hours.startTime);
    const dayEnd = this.timeToMinutes(hours.endTime);
    const appointments = this.activeAppointmentsForDoctor(doctorId, date);
    for (let start = dayStart; start + 30 <= dayEnd; start += 5) {
      const end = start + 30;
      const conflict = appointments.some((appointment) => start < this.timeToMinutes(appointment.endTime) + 30
        && end > this.timeToMinutes(appointment.startTime) - 30);
      if (!conflict) return true;
    }
    return false;
  }

  private workingDay(doctorId: number, date: string): DoctorWorkingDay | undefined {
    const day = this.isoDayOfWeek(date);
    return this.workingHours.get(doctorId)?.days.find((item) => item.dayOfWeek === day);
  }

  private slotStateForAppointment(startTime: string, appointment: AppointmentView): SlotState {
    if (!this.selectedDate || this.isSelectedDatePast) return 'unavailable';
    const start = this.timeToMinutes(startTime);
    const end = start + appointment.durationMinutes;
    if (start < this.timelineStartMinutes || end > this.timelineEndMinutes) return 'unavailable';
    const conflict = this.activeAppointmentsForDoctor(this.selectedDoctorId, this.selectedDate)
      .some((other) => other.id !== appointment.id
        && start < this.timeToMinutes(other.endTime) + 30
        && end > this.timeToMinutes(other.startTime) - 30);
    return conflict ? 'unavailable' : 'available';
  }

  private warningsFor(appointment: AppointmentView, appointments: AppointmentView[]): string[] {
    const warnings: string[] = [];
    if (!appointment.providerUserId) warnings.push('Unassigned provider');
    if (appointment.status !== 'CANCELLED') {
      const conflict = appointments.some((other) => other.id !== appointment.id
        && other.status !== 'CANCELLED'
        && other.date === appointment.date
        && this.sameProvider(appointment, other)
        && this.timeToMinutes(appointment.startTime) < this.timeToMinutes(other.endTime) + 30
        && this.timeToMinutes(appointment.endTime) > this.timeToMinutes(other.startTime) - 30);
      if (conflict) warnings.push('Schedule conflict');
    }
    return warnings;
  }

  private sameProvider(left: AppointmentView, right: AppointmentView): boolean {
    if (left.providerUserId && right.providerUserId) return left.providerUserId === right.providerUserId;
    return !left.providerUserId && !right.providerUserId
      && left.providerName.trim().toLowerCase() === right.providerName.trim().toLowerCase();
  }

  private emptyForm(): AppointmentPayload {
    return {
      patientId: 0,
      date: this.selectedDate || this.today,
      startTime: '09:00',
      endTime: '09:30',
      providerUserId: this.selectedDoctorId,
      priority: 'NORMAL',
      status: 'SCHEDULED',
      notes: '',
    };
  }

  private handleScheduleEvent(event: ScheduleEvent): void {
    const currentUser = this.sessionService.currentUser;
    if (currentUser?.role === 'doctor' && event.doctorUserId !== currentUser.id) return;
    if (event.eventType === 'WORKING_HOURS_UPDATED') {
      this.loadDoctorsAndHours();
      return;
    }
    this.loadAppointments(true);
  }

  private doctorInitials(name: string): string {
    return name.replace(/^dr\.?\s*/i, '').split(/\s+/).filter(Boolean).slice(0, 2)
      .map((part) => part[0]).join('').toUpperCase() || 'DR';
  }

  private toView(appointment: Appointment): AppointmentView {
    const patientFirstName = appointment.patientFirstName ?? appointment.clientName?.split(' ')[0] ?? '';
    const patientLastName = appointment.patientLastName ?? appointment.clientName?.split(' ').slice(1).join(' ') ?? '';
    const startTime = appointment.startTime ?? appointment.heure ?? appointment.time;
    const durationMinutes = appointment.durationMinutes ?? appointment.duration ?? 30;
    const endTime = appointment.endTime ?? this.addMinutes(startTime, durationMinutes);
    const providerName = appointment.providerName ?? appointment.dentist ?? 'Unassigned provider';
    const status = this.toApiStatus(appointment.status);
    const priority = this.toApiPriority(appointment.priority);
    const initials = `${patientFirstName[0] ?? ''}${patientLastName[0] ?? ''}`.toUpperCase();
    return {
      ...appointment,
      patientId: appointment.patientId ?? appointment.clientId,
      patientFirstName,
      patientLastName,
      startTime,
      endTime,
      heure: startTime,
      durationMinutes,
      providerName,
      priority,
      status,
      clientName: `${patientFirstName} ${patientLastName}`.trim(),
      time: startTime.slice(0, 5),
      duration: durationMinutes,
      dentist: providerName,
      color: this.colorForStatus(status),
      treatmentType: appointment.treatmentObjective
        || (appointment.treatmentId ? `Treatment #${appointment.treatmentId}` : 'General visit'),
      photo: initials || 'PT',
      warnings: [],
    };
  }

  private upsertAppointment(appointment: Appointment): void {
    this.appointments = [appointment, ...this.appointments.filter((item) => item.id !== appointment.id)];
  }

  private toApiStatus(status: string): AppointmentPayload['status'] {
    const statuses: Record<string, AppointmentPayload['status']> = {
      Confirmed: 'CONFIRMED', Waiting: 'SCHEDULED', 'In progress': 'IN_PROGRESS',
      Completed: 'COMPLETED', Cancelled: 'CANCELLED',
    };
    return statuses[status] ?? status as AppointmentPayload['status'];
  }

  private toApiPriority(priority?: string): AppointmentPayload['priority'] {
    const priorities: Record<string, AppointmentPayload['priority']> = {
      Routine: 'NORMAL', Urgent: 'URGENT', Critical: 'HIGH',
    };
    return priorities[priority ?? ''] ?? priority as AppointmentPayload['priority'] ?? 'NORMAL';
  }

  private colorForStatus(status: string): string {
    const colors: Record<AppointmentPayload['status'], string> = {
      SCHEDULED: '#1689e8', CONFIRMED: '#10b981', IN_PROGRESS: '#f59e0b',
      COMPLETED: '#64748b', CANCELLED: '#ef4444',
    };
    return colors[status as AppointmentPayload['status']] ?? '#1689e8';
  }

  private addMinutes(time: string, minutes: number): string {
    return this.minutesToTime(this.timeToMinutes(time) + minutes);
  }

  private timeToMinutes(time?: string | null): number {
    if (!time) return 0;
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  }

  private minutesToTime(minutes: number): string {
    const bounded = Math.max(0, Math.min(24 * 60 - 1, minutes));
    return `${String(Math.floor(bounded / 60)).padStart(2, '0')}:${String(bounded % 60).padStart(2, '0')}`;
  }

  private isoDayOfWeek(date: string): number {
    const day = this.parseDateKey(date).getDay();
    return day === 0 ? 7 : day;
  }

  private parseDateKey(date: string): Date {
    const [year, month, day] = date.split('-').map(Number);
    return new Date(year, month - 1, day, 12, 0, 0);
  }

  private apiError(error: any, fallback: string): string {
    const messages = error?.error?.messages;
    return Array.isArray(messages) && messages.length ? messages.join(' ') : fallback;
  }

  private formatDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
