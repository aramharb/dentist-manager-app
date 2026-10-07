import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout } from 'rxjs';
import { Patient, PatientPayload, ProcedureCatalogItem } from '../../models/secretary.models';
import { PatientService } from '../../services/patient.service';
import { ModalComponent } from '../../shared/modal/modal.component';
import { SessionService } from '../../../core/auth/session.service';
import { ClinicUser } from '../../../shared/services/message.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-secretary-clients',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './clients.component.html',
  styleUrl: './clients.component.css',
})
export class ClientsComponent implements OnInit {
  private readonly patientService = inject(PatientService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  query = '';
  sortBy: 'name' | 'lastVisit' | 'nextAppointment' | 'balance' = 'name';
  cnamFilter = '';
  patients: Patient[] = [];
  selectedPatient?: Patient;
  editingPatient?: Patient;
  detailsOpen = false;
  formOpen = false;
  loading = false;
  saving = false;
  deleting = false;
  catalogLoading = false;
  errorMessage = '';
  formError = '';
  catalogError = '';
  treatmentCatalog: ProcedureCatalogItem[] = [];
  doctors: ClinicUser[] = [];
  doctorsLoading = false;
  doctorsError = '';
  treatmentSelection: number | 'other' | '' = '';
  formModel: PatientPayload = this.emptyForm();

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.loadPatients();
    this.loadTreatmentCatalog();
    this.loadDoctors();
  }

  get filteredPatients(): Patient[] {
    const value = this.query.toLowerCase();
    return [...this.patients]
      .filter((patient) => !this.cnamFilter || String(patient.cnamCovered) === this.cnamFilter)
      .filter((patient) =>
        `${patient.patientNumber} ${patient.firstName} ${patient.lastName} ${patient.phoneNumber} ${patient.email || ''}`
          .toLowerCase()
          .includes(value),
      )
      .sort((a, b) => {
        if (this.sortBy === 'balance') {
          return (b.unpaidBalance || 0) - (a.unpaidBalance || 0);
        }
        if (this.sortBy === 'lastVisit') {
          return this.safeDate(b.lastVisit).localeCompare(this.safeDate(a.lastVisit));
        }
        if (this.sortBy === 'nextAppointment') {
          return this.safeDate(a.nextAppointment).localeCompare(this.safeDate(b.nextAppointment));
        }
        return `${a.lastName} ${a.firstName}`.localeCompare(`${b.lastName} ${b.firstName}`);
      });
  }

  loadPatients(): void {
    this.loading = true;
    this.errorMessage = '';
    this.patientService
      .getPatients()
      .pipe(
        timeout(10000),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (patients) => (this.patients = patients),
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.toErrorMessage(error, 'Unable to load patients.');
        },
      });
  }

  openDetails(patient: Patient): void {
    this.selectedPatient = patient;
    this.detailsOpen = true;
  }

  openForm(patient?: Patient): void {
    this.editingPatient = patient;
    this.formError = '';
    this.formModel = patient ? this.toFormModel(patient) : this.emptyForm();
    if (!patient && !this.isDoctor && this.doctors.length === 1) {
      this.formModel.assignedDoctorUserId = this.doctors[0].id;
    }
    this.treatmentSelection = patient?.selectedTreatmentId
      ? patient.selectedTreatmentId
      : patient?.currentTreatment
        ? 'other'
        : '';
    this.formOpen = true;
  }

  get remainingAmount(): number {
    return Math.max(0, Number(this.formModel.expectedAmount || 0) - Number(this.formModel.paidAmount || 0));
  }

  get canDeletePatients(): boolean {
    return this.session.currentUser?.role === 'doctor';
  }

  get isDoctor(): boolean {
    return this.session.currentUser?.role === 'doctor';
  }

  onTreatmentChange(): void {
    if (typeof this.treatmentSelection === 'number') {
      const treatment = this.treatmentCatalog.find((item) => item.id === this.treatmentSelection);
      this.formModel.selectedTreatmentId = treatment?.id ?? null;
      this.formModel.currentTreatment = treatment?.name ?? '';
      this.formModel.expectedAmount = treatment?.defaultCost ?? 0;
    } else if (this.treatmentSelection === 'other') {
      this.formModel.selectedTreatmentId = null;
      this.formModel.currentTreatment = '';
      this.formModel.expectedAmount = 0;
    } else {
      this.formModel.selectedTreatmentId = null;
      this.formModel.currentTreatment = '';
      this.formModel.expectedAmount = 0;
    }
    this.formModel.paidAmount = Math.min(Number(this.formModel.paidAmount || 0), this.formModel.expectedAmount);
  }

  scheduleNextAppointment(): void {
    if (!this.editingPatient || this.isDoctor) return;
    this.formOpen = false;
    this.openRendezVousFor(this.editingPatient);
  }

  savePatient(form: NgForm, scheduleAfterSave = false): void {
    if (form.invalid) {
      form.control.markAllAsTouched();
      this.formError = 'Please complete the required patient fields.';
      return;
    }

    this.saving = true;
    this.formError = '';
    const payload = this.cleanPayload(this.formModel);
    const request = this.editingPatient
      ? this.patientService.updatePatient(this.editingPatient.id, payload)
      : this.patientService.createPatient(payload);

    request.pipe(finalize(() => {
      this.saving = false;
      this.cdr.markForCheck();
    })).subscribe({
      next: (patient) => {
        this.upsertPatient(patient);
        this.selectedPatient = patient;
        this.formOpen = false;
        this.detailsOpen = !scheduleAfterSave;
        if (scheduleAfterSave && !this.isDoctor) this.openRendezVousFor(patient);
      },
      error: (error: HttpErrorResponse) => {
        this.formError = this.toErrorMessage(error, 'Unable to save patient.');
      },
    });
  }

  private upsertPatient(patient: Patient): void {
    const index = this.patients.findIndex((item) => item.id === patient.id);
    this.patients =
      index >= 0
        ? this.patients.map((item) => (item.id === patient.id ? patient : item))
        : [patient, ...this.patients];
  }

  private openRendezVousFor(patient: Patient): void {
    void this.router.navigate(['/secretaire/appointments'], {
      queryParams: {
        patientId: patient.id,
        patientName: `${patient.firstName} ${patient.lastName}`,
      },
    });
  }

  loadTreatmentCatalog(): void {
    this.catalogLoading = true;
    this.catalogError = '';
    this.patientService
      .getTreatmentCatalog()
      .pipe(finalize(() => {
        this.catalogLoading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (items) => (this.treatmentCatalog = items),
        error: () => (this.catalogError = 'Unable to load treatment prices from the database.'),
      });
  }

  loadDoctors(): void {
    this.doctorsLoading = true;
    this.doctorsError = '';
    this.patientService
      .getDoctors()
      .pipe(finalize(() => {
        this.doctorsLoading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (users) => {
          this.doctors = users.filter((user) => user.role === 'doctor');
          if (!this.isDoctor && !this.formModel.assignedDoctorUserId && this.doctors.length === 1) {
            this.formModel.assignedDoctorUserId = this.doctors[0].id;
          }
        },
        error: () => (this.doctorsError = 'Unable to load the active doctors.'),
      });
  }

  deletePatient(patient: Patient): void {
    if (!this.canDeletePatients) {
      this.errorMessage = 'Only a doctor can delete patients.';
      return;
    }
    if (this.remainingBalance(patient) > 0) {
      this.errorMessage = 'This patient cannot be deleted while a balance remains to be paid.';
      return;
    }
    if (!isPlatformBrowser(this.platformId) || !window.confirm(`Delete ${patient.firstName} ${patient.lastName}? This cannot be undone.`)) {
      return;
    }

    this.deleting = true;
    this.formError = '';
    this.patientService.deletePatient(patient.id).pipe(finalize(() => {
      this.deleting = false;
      this.cdr.markForCheck();
    })).subscribe({
      next: () => {
        this.patients = this.patients.filter((item) => item.id !== patient.id);
        this.detailsOpen = false;
        this.selectedPatient = undefined;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.toErrorMessage(error, 'Unable to delete patient.');
      },
    });
  }

  private emptyForm(): PatientPayload {
    return {
      patientNumber: '',
      firstName: '',
      lastName: '',
      gender: '',
      birthDate: '',
      address: '',
      phoneNumber: '',
      email: '',
      bloodType: '',
      allergies: '',
      currentTreatment: '',
      selectedTreatmentId: null,
      expectedAmount: 0,
      paidAmount: 0,
      cnamCovered: false,
      cnamNumber: '',
      firstVisit: new Date().toISOString().slice(0, 10),
      assignedDoctorUserId: null,
      notes: '',
    };
  }

  private toFormModel(patient: Patient): PatientPayload {
    return {
      patientNumber: patient.patientNumber || '',
      firstName: patient.firstName || '',
      lastName: patient.lastName || '',
      gender: patient.gender || '',
      birthDate: patient.birthDate || '',
      address: patient.address || '',
      phoneNumber: patient.phoneNumber || '',
      email: patient.email || '',
      bloodType: patient.bloodType || '',
      allergies: patient.allergies || '',
      currentTreatment: patient.currentTreatment || '',
      selectedTreatmentId: patient.selectedTreatmentId ?? null,
      expectedAmount: Number(patient.expectedAmount || 0),
      paidAmount: Number(patient.paidAmount || 0),
      cnamCovered: Boolean(patient.cnamCovered),
      cnamNumber: patient.cnamNumber || '',
      firstVisit: patient.firstVisit || '',
      assignedDoctorUserId: patient.assignedDoctorUserId ?? null,
      notes: patient.notes || '',
    };
  }

  private cleanPayload(payload: PatientPayload): PatientPayload {
    return {
      ...payload,
      patientNumber: payload.patientNumber?.trim() || '',
      firstName: payload.firstName.trim(),
      lastName: payload.lastName.trim(),
      birthDate: payload.birthDate || undefined,
      phoneNumber: payload.phoneNumber.trim(),
      email: payload.email?.trim() || '',
      bloodType: payload.bloodType || undefined,
      cnamNumber: payload.cnamCovered ? payload.cnamNumber?.trim() || '' : '',
      currentTreatment: payload.currentTreatment?.trim() || '',
      selectedTreatmentId: payload.selectedTreatmentId ?? null,
      expectedAmount: Number(payload.expectedAmount || 0),
      paidAmount: Number(payload.paidAmount || 0),
      assignedDoctorUserId: payload.assignedDoctorUserId ?? null,
    };
  }

  private safeDate(value?: string): string {
    return value || '9999-12-31';
  }

  remainingBalance(patient: Patient): number {
    return Math.max(0, Number(patient.expectedAmount || 0) - Number(patient.paidAmount || 0));
  }

  private toErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const messages = error.error?.messages;
    return Array.isArray(messages) && messages.length ? messages.join(' ') : fallback;
  }
}
