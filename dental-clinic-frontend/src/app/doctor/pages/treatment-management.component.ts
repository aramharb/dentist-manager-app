import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { ToothSelectorComponent } from '../components/tooth-selector/tooth-selector.component';
import {
  PhotoType,
  Prescription,
  ProcedureCatalogItem,
  ProcedurePayload,
  ProcedureStatus,
  Treatment,
  TreatmentHistory,
  TreatmentPatientSummary,
  TreatmentPhoto,
  TreatmentPriority,
  TreatmentProcedure,
  TreatmentStatus,
} from '../models/treatment.models';
import { TreatmentService } from '../services/treatment.service';

interface PendingProcedure {
  label: string;
  payload: ProcedurePayload;
}

@Component({
  selector: 'app-treatment-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ToothSelectorComponent],
  templateUrl: './treatment-management.component.html',
  styleUrl: './treatment-management.component.css',
})
export class TreatmentManagementComponent implements OnInit {
  private readonly service = inject(TreatmentService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly fb = inject(FormBuilder);

  patients: TreatmentPatientSummary[] = [];
  treatments: Treatment[] = [];
  procedures: TreatmentProcedure[] = [];
  catalog: ProcedureCatalogItem[] = [];
  timeline: TreatmentHistory[] = [];
  photos: TreatmentPhoto[] = [];
  prescriptions: Prescription[] = [];
  pendingProcedures: PendingProcedure[] = [];

  selectedPatient: TreatmentPatientSummary | null = null;
  selectedTreatment: Treatment | null = null;
  editingCatalog: ProcedureCatalogItem | null = null;
  selectedTeeth: number[] = [];
  allTeeth = false;
  patientQuery = '';
  planMode: 'create' | 'edit' = 'create';
  catalogEditorOpen = false;
  procedureEditorOpen = false;
  patientsLoading = false;
  patientsError = '';
  savingPlan = false;
  toast = '';

  readonly treatmentForm = this.fb.nonNullable.group({
    objective: ['', Validators.required],
    status: ['PLANNED' as TreatmentStatus],
    priority: ['NORMAL' as TreatmentPriority],
    estimatedBill: [0, [Validators.min(0)]],
    paidAmount: [0, [Validators.min(0)]],
    estimatedDurationMinutes: [0, [Validators.min(0)]],
    doctorNotes: [''],
  });

  readonly procedureForm = this.fb.nonNullable.group({
    catalogQuery: [''],
    procedureCatalogId: [null as number | null],
    name: ['', Validators.required],
    status: ['PLANNED' as ProcedureStatus],
    cost: [0, [Validators.min(0)]],
    practitioner: [''],
    durationMinutes: [30, [Validators.min(1)]],
    notes: [''],
  });

  readonly toothDescriptionForm = this.fb.group({
    allTeethDescription: [''],
    descriptions: this.fb.array([]),
  });

  readonly catalogForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    category: ['', Validators.required],
    defaultCost: [0, [Validators.min(0)]],
    defaultDurationMinutes: [30, [Validators.min(1)]],
    description: [''],
    active: [true],
  });

  readonly photoForm = this.fb.nonNullable.group({
    photoType: ['BEFORE' as PhotoType],
    fileName: ['', Validators.required],
    url: ['', Validators.required],
    description: [''],
  });

  readonly medicineForm = this.fb.nonNullable.group({
    medicineName: ['', Validators.required],
    dosage: ['', Validators.required],
    duration: ['', Validators.required],
    instructions: [''],
  });

  readonly medicines = this.fb.array<ReturnType<typeof this.createMedicineGroup>>([]);

  readonly historyForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
  });

  get isDoctor(): boolean {
    return this.session.currentUser?.role === 'doctor';
  }

  get filteredPatients(): TreatmentPatientSummary[] {
    const query = this.patientQuery.trim().toLowerCase();
    if (!query) return this.patients;
    return this.patients.filter((patient) =>
      `${patient.firstName} ${patient.lastName} ${patient.patientNumber} ${patient.phoneNumber ?? ''}`
        .toLowerCase()
        .includes(query),
    );
  }

  get toothDescriptions(): FormArray {
    return this.toothDescriptionForm.controls.descriptions;
  }

  get procedureScopeValid(): boolean {
    return this.allTeeth || this.selectedTeeth.length > 0;
  }

  get totalProcedureCount(): number {
    return this.procedures.length + this.pendingProcedures.length;
  }

  get completedProcedureCount(): number {
    return [...this.procedures, ...this.pendingProcedures.map((item) => item.payload)].filter(
      (item) => item.status === 'COMPLETED',
    ).length;
  }

  get calculatedProgress(): number {
    return this.totalProcedureCount
      ? Math.round((this.completedProcedureCount / this.totalProcedureCount) * 100)
      : 0;
  }

  get planRemaining(): number {
    return Math.max(
      0,
      Number(this.treatmentForm.controls.estimatedBill.value || 0) -
        Number(this.treatmentForm.controls.paidAmount.value || 0),
    );
  }

  get plannedProcedureCost(): number {
    return [...this.procedures, ...this.pendingProcedures.map((item) => item.payload)]
      .filter((item) => item.status !== 'CANCELLED')
      .reduce((total, item) => total + Number(item.cost || 0), 0);
  }

  get completedProcedureValue(): number {
    return [...this.procedures, ...this.pendingProcedures.map((item) => item.payload)]
      .filter((item) => item.status === 'COMPLETED')
      .reduce((total, item) => total + Number(item.cost || 0), 0);
  }

  get remainingProcedureValue(): number {
    return Math.max(0, this.plannedProcedureCost - this.completedProcedureValue);
  }

  ngOnInit(): void {
    this.loadPatients();
    this.loadCatalog();
  }

  backToPatients(): void {
    void this.router.navigate(['/doctor/clients']);
  }

  selectPatient(patient: TreatmentPatientSummary): void {
    this.selectedPatient = patient;
    this.patientQuery = '';
    this.resetClinicalRecord();
    this.service.getPatientTreatments(patient.id).subscribe({
      next: (treatments) => {
        this.treatments = treatments;
        if (treatments.length) this.openTreatment(treatments[0]);
        else this.startNewTreatment();
      },
      error: (error) => this.showError(error, 'Could not load the patient treatments.'),
    });
  }

  startNewTreatment(): void {
    this.selectedTreatment = null;
    this.planMode = 'create';
    this.procedures = [];
    this.timeline = [];
    this.photos = [];
    this.prescriptions = [];
    this.pendingProcedures = [];
    this.medicines.clear();
    this.treatmentForm.reset({
      objective: this.selectedPatient?.currentTreatment ?? '',
      status: 'PLANNED',
      priority: 'NORMAL',
      estimatedBill: 0,
      paidAmount: 0,
      estimatedDurationMinutes: 0,
      doctorNotes: '',
    });
  }

  openTreatment(treatment: Treatment): void {
    this.selectedTreatment = treatment;
    this.planMode = 'edit';
    this.pendingProcedures = [];
    this.medicines.clear();
    this.patchTreatmentForm(treatment);
    this.loadTreatmentDetails(treatment.id);
  }

  async saveAllData(): Promise<void> {
    if (!this.selectedPatient || this.treatmentForm.invalid || this.savingPlan) {
      this.treatmentForm.markAllAsTouched();
      return;
    }
    if (this.planRemaining < 0) return;

    this.savingPlan = true;
    try {
      const value = this.treatmentForm.getRawValue();
      const payload = {
        ...value,
        progressPercent: this.calculatedProgress,
      };
      const treatment = this.selectedTreatment
        ? await firstValueFrom(this.service.updateTreatment(this.selectedTreatment.id, payload))
        : await firstValueFrom(this.service.createTreatment(this.selectedPatient.id, payload));

      for (const pending of this.pendingProcedures) {
        await firstValueFrom(this.service.addProcedure(treatment.id, pending.payload));
      }
      for (const medicine of this.medicines.controls) {
        await firstValueFrom(
          this.service.addPrescription(treatment.id, {
            ...medicine.getRawValue(),
            issuedAt: new Date().toISOString(),
            pdfUrl: null,
          }),
        );
      }

      this.pendingProcedures = [];
      this.medicines.clear();
      await this.reloadPatientTreatments(treatment.id);
      this.notify('Treatment data saved.');
    } catch (error) {
      this.showError(error, 'Could not save the treatment data.');
    } finally {
      this.savingPlan = false;
    }
  }

  openProcedureEditor(): void {
    if (!this.procedureScopeValid) return;
    this.procedureEditorOpen = true;
    this.procedureForm.reset({
      catalogQuery: '',
      procedureCatalogId: null,
      name: '',
      status: 'PLANNED',
      cost: 0,
      practitioner: this.session.currentUser?.fullName ?? '',
      durationMinutes: 30,
      notes: '',
    });
    this.syncToothDescriptionFields();
    this.loadCatalog();
  }

  syncToothDescriptionFields(): void {
    this.toothDescriptions.clear();
    if (!this.allTeeth) {
      this.selectedTeeth.forEach((toothNumber) =>
        this.toothDescriptions.push(
          this.fb.nonNullable.group({ toothNumber, description: '' }),
        ),
      );
    }
  }

  saveSelectedTeeth(): void {
    if (!this.procedureScopeValid || this.procedureForm.invalid) {
      this.procedureForm.markAllAsTouched();
      return;
    }
    const value = this.procedureForm.getRawValue();
    const base: Omit<ProcedurePayload, 'toothNumber' | 'allTeeth' | 'toothDescription'> = {
      procedureCatalogId: value.procedureCatalogId,
      name: value.name.trim(),
      status: value.status,
      cost: Number(value.cost || 0),
      practitioner: value.practitioner.trim() || null,
      durationMinutes: Number(value.durationMinutes || 30),
      notes: value.notes.trim() || null,
    };

    if (this.allTeeth) {
      this.pendingProcedures.push({
        label: 'All teeth',
        payload: {
          ...base,
          toothNumber: null,
          allTeeth: true,
          toothDescription: this.toothDescriptionForm.controls.allTeethDescription.value?.trim() || null,
        },
      });
    } else {
      this.toothDescriptions.controls.forEach((control) => {
        const toothNumber = Number(control.get('toothNumber')?.value);
        this.pendingProcedures.push({
          label: `Tooth ${toothNumber}`,
          payload: {
            ...base,
            toothNumber,
            allTeeth: false,
            toothDescription: String(control.get('description')?.value ?? '').trim() || null,
          },
        });
      });
    }
    this.procedureEditorOpen = false;
    this.notify('Procedure added to the form. Use Save all treatment data to persist it.');
  }

  removePendingProcedure(index: number): void {
    this.pendingProcedures.splice(index, 1);
  }

  selectCatalog(item: ProcedureCatalogItem): void {
    this.procedureForm.patchValue({
      procedureCatalogId: item.id,
      name: item.name,
      cost: item.defaultCost,
      durationMinutes: item.defaultDurationMinutes,
    });
  }

  searchCatalog(): void {
    this.loadCatalog(this.procedureForm.controls.catalogQuery.value);
  }

  updateProcedureStatus(procedure: TreatmentProcedure, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as ProcedureStatus;
    const payload: ProcedurePayload = {
      procedureCatalogId: procedure.procedureCatalogId,
      toothNumber: procedure.toothNumber,
      allTeeth: procedure.allTeeth,
      toothDescription: procedure.toothDescription,
      name: procedure.name,
      status,
      practitioner: procedure.practitioner,
      cost: procedure.cost,
      durationMinutes: procedure.durationMinutes,
      startedAt: procedure.startedAt,
      completedAt: status === 'COMPLETED' ? new Date().toISOString() : null,
      notes: procedure.notes,
    };
    this.service.updateProcedure(procedure.id, payload).subscribe({
      next: () => this.selectedTreatment && this.refreshSelectedTreatment(),
      error: (error) => this.showError(error, 'Could not update the procedure.'),
    });
  }

  deleteProcedure(procedure: TreatmentProcedure): void {
    this.service.deleteProcedure(procedure.id).subscribe({
      next: () => {
        this.procedures = this.procedures.filter((item) => item.id !== procedure.id);
        this.refreshSelectedTreatment();
        this.notify('Procedure deleted.');
      },
      error: (error) => this.showError(error, 'Could not delete the procedure.'),
    });
  }

  openCatalogEditor(item?: ProcedureCatalogItem): void {
    this.catalogEditorOpen = true;
    this.editingCatalog = item ?? null;
    this.catalogForm.reset(
      item
        ? {
            name: item.name,
            code: item.code,
            category: item.category,
            defaultCost: item.defaultCost,
            defaultDurationMinutes: item.defaultDurationMinutes,
            description: item.description ?? '',
            active: item.active !== false,
          }
        : {
            name: '',
            code: '',
            category: '',
            defaultCost: 0,
            defaultDurationMinutes: 30,
            description: '',
            active: true,
          },
    );
    this.loadCatalog('', true);
  }

  saveCatalogItem(): void {
    if (this.catalogForm.invalid) {
      this.catalogForm.markAllAsTouched();
      return;
    }
    const request = this.editingCatalog
      ? this.service.updateCatalogItem(this.editingCatalog.id, this.catalogForm.getRawValue())
      : this.service.createCatalogItem(this.catalogForm.getRawValue());
    request.subscribe({
      next: () => {
        this.editingCatalog = null;
        this.catalogEditorOpen = false;
        this.loadCatalog();
        this.notify('Catalog treatment saved.');
      },
      error: (error) => this.showError(error, 'Could not save the catalog treatment.'),
    });
  }

  deleteCatalogItem(item: ProcedureCatalogItem): void {
    this.service.deleteCatalogItem(item.id).subscribe({
      next: () => {
        this.loadCatalog('', true);
        this.notify('Catalog treatment deactivated.');
      },
      error: (error) => this.showError(error, 'Could not deactivate the catalog treatment.'),
    });
  }

  addPhoto(): void {
    if (!this.selectedTreatment || this.photoForm.invalid) return;
    const value = this.photoForm.getRawValue();
    this.service
      .addPhoto(this.selectedTreatment.id, {
        ...value,
        contentType: this.contentTypeFor(value.fileName),
        uploadedBy: this.session.currentUser?.fullName ?? null,
      })
      .subscribe({
        next: (photo) => {
          this.photos = [photo, ...this.photos];
          this.photoForm.reset({ photoType: 'BEFORE', fileName: '', url: '', description: '' });
          this.notify('Photo saved.');
        },
        error: (error) => this.showError(error, 'Could not save the photo.'),
      });
  }

  photosOfType(type: PhotoType): TreatmentPhoto[] {
    return this.photos.filter((photo) => photo.photoType === type);
  }

  otherPhotos(): TreatmentPhoto[] {
    return this.photos.filter((photo) => !['BEFORE', 'AFTER'].includes(photo.photoType));
  }

  photoSrc(photo: TreatmentPhoto): string {
    return photo.url;
  }

  markPhotoBroken(event: Event): void {
    (event.target as HTMLImageElement).hidden = true;
  }

  addMedicine(): void {
    if (this.medicineForm.invalid) {
      this.medicineForm.markAllAsTouched();
      return;
    }
    this.medicines.push(this.createMedicineGroup(this.medicineForm.getRawValue()));
    this.medicineForm.reset({ medicineName: '', dosage: '', duration: '', instructions: '' });
  }

  removeMedicine(index: number): void {
    this.medicines.removeAt(index);
  }

  exportMedicine(index: number): void {
    this.printPrescription(this.medicines.at(index).getRawValue());
  }

  exportPrescription(prescription: Prescription): void {
    this.printPrescription(prescription);
  }

  addHistory(): void {
    if (!this.selectedTreatment || this.historyForm.invalid) return;
    const value = this.historyForm.getRawValue();
    this.service
      .addHistory(this.selectedTreatment.id, {
        eventType: 'VISIT',
        title: value.title,
        description: value.description,
        eventAt: new Date().toISOString(),
        createdBy: this.session.currentUser?.fullName,
      })
      .subscribe({
        next: (entry) => {
          this.timeline = [entry, ...this.timeline];
          this.historyForm.reset({ title: '', description: '' });
          this.notify('Visit note saved.');
        },
        error: (error) => this.showError(error, 'Could not save the visit note.'),
      });
  }

  toothLabel(toothNumber: number): string {
    const quadrant = Math.floor(toothNumber / 10);
    const position = toothNumber % 10;
    const sides: Record<number, string> = {
      1: 'upper right',
      2: 'upper left',
      3: 'lower left',
      4: 'lower right',
    };
    return `${sides[quadrant] ?? 'tooth'} position ${position}`;
  }

  private loadPatients(): void {
    this.patientsLoading = true;
    this.patientsError = '';
    this.service.getMyPatients().subscribe({
      next: (patients) => {
        this.patients = patients;
        this.patientsLoading = false;
      },
      error: (error) => {
        this.patientsLoading = false;
        this.patientsError = 'Could not load clients.';
        this.showError(error, this.patientsError);
      },
    });
  }

  private loadCatalog(query = '', includeInactive = false): void {
    this.service.searchCatalog(query, includeInactive).subscribe({
      next: (catalog) => (this.catalog = catalog),
      error: (error) => this.showError(error, 'Could not load the treatment catalog.'),
    });
  }

  private loadTreatmentDetails(treatmentId: number): void {
    this.service.getProcedures(treatmentId).subscribe({
      next: (items) => (this.procedures = items),
      error: (error) => this.showError(error, 'Could not load procedures.'),
    });
    this.service.getTimeline(treatmentId).subscribe({
      next: (items) => (this.timeline = items),
      error: (error) => this.showError(error, 'Could not load the visit timeline.'),
    });
    this.service.getPhotos(treatmentId).subscribe({
      next: (items) => (this.photos = items),
      error: (error) => this.showError(error, 'Could not load photos.'),
    });
    this.service.getPrescriptions(treatmentId).subscribe({
      next: (items) => (this.prescriptions = items),
      error: (error) => this.showError(error, 'Could not load prescriptions.'),
    });
  }

  private refreshSelectedTreatment(): void {
    if (!this.selectedTreatment) return;
    this.service.getTreatment(this.selectedTreatment.id).subscribe({
      next: (treatment) => {
        this.selectedTreatment = treatment;
        this.patchTreatmentForm(treatment);
        this.loadTreatmentDetails(treatment.id);
      },
      error: (error) => this.showError(error, 'Could not refresh the treatment.'),
    });
  }

  private async reloadPatientTreatments(selectedId: number): Promise<void> {
    if (!this.selectedPatient) return;
    this.treatments = await firstValueFrom(this.service.getPatientTreatments(this.selectedPatient.id));
    const selected = this.treatments.find((item) => item.id === selectedId) ?? this.treatments[0];
    if (selected) this.openTreatment(selected);
    this.loadPatients();
  }

  private patchTreatmentForm(treatment: Treatment): void {
    this.treatmentForm.reset({
      objective: treatment.objective,
      status: treatment.status,
      priority: treatment.priority,
      estimatedBill: Number(treatment.estimatedBill || 0),
      paidAmount: Number(treatment.paidAmount || 0),
      estimatedDurationMinutes: Number(treatment.estimatedDurationMinutes || 0),
      doctorNotes: treatment.doctorNotes ?? '',
    });
  }

  private resetClinicalRecord(): void {
    this.treatments = [];
    this.procedures = [];
    this.timeline = [];
    this.photos = [];
    this.prescriptions = [];
    this.pendingProcedures = [];
    this.selectedTreatment = null;
    this.selectedTeeth = [];
    this.allTeeth = false;
  }

  private createMedicineGroup(value?: {
    medicineName: string;
    dosage: string;
    duration: string;
    instructions: string;
  }) {
    return this.fb.nonNullable.group({
      medicineName: [value?.medicineName ?? '', Validators.required],
      dosage: [value?.dosage ?? '', Validators.required],
      duration: [value?.duration ?? '', Validators.required],
      instructions: [value?.instructions ?? ''],
    });
  }

  private printPrescription(prescription: {
    medicineName: string;
    dosage: string;
    duration: string;
    instructions?: string | null;
  }): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const popup = window.open('', '_blank', 'noopener,noreferrer');
    if (!popup) {
      this.notify('Allow pop-ups to print the prescription.');
      return;
    }
    popup.document.write(
      `<title>Prescription</title><main style="font-family:Arial;padding:32px"><h1>Prescription</h1><h2>${this.escapeHtml(prescription.medicineName)}</h2><p><b>Dosage:</b> ${this.escapeHtml(prescription.dosage)}</p><p><b>Duration:</b> ${this.escapeHtml(prescription.duration)}</p><p>${this.escapeHtml(prescription.instructions ?? '')}</p></main>`,
    );
    popup.document.close();
    popup.print();
  }

  private contentTypeFor(fileName: string): string {
    const extension = fileName.split('.').pop()?.toLowerCase();
    if (extension === 'png') return 'image/png';
    if (extension === 'webp') return 'image/webp';
    return 'image/jpeg';
  }

  private escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (character) => {
      const entities: Record<string, string> = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;',
      };
      return entities[character];
    });
  }

  private showError(error: unknown, fallback: string): void {
    const response = error as HttpErrorResponse;
    const message =
      response.error?.message ??
      response.error?.messages?.join?.(' ') ??
      (typeof response.error === 'string' ? response.error : null) ??
      fallback;
    this.notify(message);
  }

  private notify(message: string): void {
    this.toast = message;
    if (isPlatformBrowser(this.platformId)) {
      window.setTimeout(() => {
        if (this.toast === message) this.toast = '';
      }, 4000);
    }
  }
}
