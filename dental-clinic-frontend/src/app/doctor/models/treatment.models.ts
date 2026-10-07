export type TreatmentStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';
export type TreatmentPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type ProcedureStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type HistoryEventType = 'CREATED' | 'VISIT' | 'PHOTO_ADDED' | 'PROCEDURE_COMPLETED' | 'PRESCRIPTION_GENERATED' | 'PAYMENT' | 'COMPLETED' | 'NOTE';
export type PhotoType = 'BEFORE' | 'AFTER' | 'XRAY' | 'SCAN' | 'OTHER';

export interface Treatment {
  id: number;
  patientId: number;
  doctorUserId: number;
  doctorName: string;
  treatmentTypeId?: number | null;
  treatmentTypeName?: string | null;
  objective: string;
  status: TreatmentStatus;
  priority: TreatmentPriority;
  progressPercent: number;
  estimatedDurationMinutes: number;
  estimatedBill: number;
  paidAmount: number;
  remainingBalance: number;
  upcomingAppointment?: string | null;
  lastVisit?: string | null;
  doctorNotes?: string | null;
  assignedTeeth: number[];
  hasWholeMouthProcedure: boolean;
  procedureCount: number;
  completedProcedureCount: number;
  remainingProcedureCount: number;
  plannedTreatmentCost: number;
  completedTreatmentValue: number;
  remainingTreatmentValue: number;
  createdAt: string;
  updatedAt: string;
}

export interface TreatmentPayload {
  treatmentTypeId?: number | null;
  objective: string;
  status: TreatmentStatus;
  priority: TreatmentPriority;
  progressPercent?: number;
  estimatedDurationMinutes?: number;
  estimatedBill?: number;
  paidAmount?: number;
  upcomingAppointment?: string | null;
  lastVisit?: string | null;
  doctorNotes?: string | null;
}

export interface TreatmentProcedure {
  id: number;
  treatmentId: number;
  procedureCatalogId?: number | null;
  toothNumber?: number | null;
  allTeeth: boolean;
  toothDescription?: string | null;
  name: string;
  status: ProcedureStatus;
  practitioner?: string | null;
  cost: number;
  durationMinutes: number;
  startedAt?: string | null;
  completedAt?: string | null;
  completedByUserId?: number | null;
  completedByName?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProcedurePayload {
  procedureCatalogId?: number | null;
  toothNumber?: number | null;
  allTeeth: boolean;
  toothDescription?: string | null;
  name: string;
  status: ProcedureStatus;
  practitioner?: string | null;
  cost: number;
  durationMinutes: number;
  startedAt?: string | null;
  completedAt?: string | null;
  notes?: string | null;
}

export interface TreatmentPatientSummary {
  id: number;
  patientNumber: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  currentTreatment?: string | null;
  treatmentStatus?: TreatmentStatus | null;
  progressPercent: number;
  assignedDoctorUserId: number;
  assignedDoctorName: string;
  lastVisit?: string | null;
  paidAmount?: number;
  expectedAmount?: number;
  unpaidBalance?: number;
}

export interface ProcedureCatalogItem {
  id: number;
  code: string;
  name: string;
  category: string;
  defaultCost: number;
  defaultDurationMinutes: number;
  description?: string | null;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProcedureCatalogPayload {
  code?: string;
  name: string;
  category?: string;
  defaultCost: number;
  defaultDurationMinutes: number;
  description?: string | null;
  active?: boolean;
}

export interface TreatmentHistory {
  id: number;
  treatmentId: number;
  eventType: HistoryEventType;
  title: string;
  description?: string | null;
  eventAt: string;
  createdBy?: string | null;
}

export interface TreatmentPhoto {
  id: number;
  treatmentId: number;
  photoType: PhotoType;
  fileName: string;
  contentType: string;
  url: string;
  description?: string | null;
  uploadedBy?: string | null;
  uploadedAt: string;
}

export interface Prescription {
  id: number;
  treatmentId: number;
  medicineName: string;
  dosage: string;
  duration: string;
  instructions?: string | null;
  issuedAt: string;
  pdfUrl?: string | null;
}

export type PrescriptionPayload = Omit<Prescription, 'id' | 'treatmentId'>;
