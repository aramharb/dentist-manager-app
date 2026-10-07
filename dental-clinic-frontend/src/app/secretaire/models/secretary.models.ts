export interface ContactInformation {
  phone: string;
  email: string;
  address: string;
  emergencyContact?: string;
}

export interface Client {
  id: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  age: number;
  gender: 'Female' | 'Male';
  contact: ContactInformation;
  bloodType: string;
  allergies: string;
  medicalHistory: string;
  currentMedications: string;
  assignedDentist: string;
  lastAppointment: string;
  nextAppointment: string;
  status: 'Active' | 'New' | 'Follow-up' | 'Paused';
  mainComplaint: string;
  treatmentProgress: number;
  notes: string;
  avatar: string;
  insuranceProvider?: string;
  billingBalance?: number;
  lifetimeValue?: number;
  documents?: number;
}

export interface Patient {
  id: number;
  patientNumber: string;
  firstName: string;
  lastName: string;
  gender?: string;
  birthDate?: string;
  address?: string;
  phoneNumber: string;
  email?: string;
  bloodType?: string;
  allergies?: string;
  currentTreatment?: string;
  selectedTreatmentId?: number | null;
  expectedAmount: number;
  paidAmount: number;
  cnamCovered: boolean;
  cnamNumber?: string;
  firstVisit?: string;
  lastVisit?: string;
  nextAppointment?: string;
  unpaidBalance: number;
  registrationTreatmentId?: number | null;
  treatmentStatus?: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED' | null;
  treatmentPriority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT' | null;
  treatmentProgressPercent?: number | null;
  assignedDoctorUserId?: number | null;
  assignedDoctorName?: string | null;
  notes?: string;
  lastModified?: string;
}

export type PatientPayload = Omit<Patient,
  | 'id'
  | 'lastModified'
  | 'lastVisit'
  | 'nextAppointment'
  | 'unpaidBalance'
  | 'registrationTreatmentId'
  | 'treatmentStatus'
  | 'treatmentPriority'
  | 'treatmentProgressPercent'
  | 'assignedDoctorName'
>;

export interface ProcedureCatalogItem {
  id: number;
  code: string;
  name: string;
  category: string;
  defaultCost: number;
  defaultDurationMinutes: number;
  description?: string | null;
}

export type AppointmentPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT' | 'Routine' | 'Urgent' | 'Critical';
export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'Confirmed' | 'Waiting' | 'In progress' | 'Completed' | 'Cancelled';

export interface Appointment {
  id: number;
  patientId?: number;
  patientFirstName?: string;
  patientLastName?: string;
  treatmentId?: number;
  date: string;
  startTime?: string;
  endTime?: string;
  heure?: string;
  durationMinutes?: number;
  providerName?: string;
  providerUserId?: number | null;
  treatmentObjective?: string | null;
  priority?: AppointmentPriority;
  status: AppointmentStatus;
  notes?: string;
  clientId: number;
  clientName: string;
  time: string;
  duration: number;
  treatmentType: string;
  dentist: string;
  color: string;
  room?: string;
  photo?: string;
}

export interface AppointmentPayload {
  patientId: number;
  treatmentId?: number;
  date: string;
  startTime: string;
  endTime: string;
  providerUserId: number;
  heure?: string;
  durationMinutes?: number;
  providerName?: string;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  status: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS';
  notes?: string;
}

export interface AppointmentView extends Appointment {
  patientId: number;
  heure: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  providerName: string;
  priority: AppointmentPayload['priority'];
  status: AppointmentPayload['status'];
  clientName: string;
  time: string;
  duration: number;
  dentist: string;
  color: string;
  treatmentType: string;
  notes?: string;
  photo?: string;
  warnings: string[];
}

export interface DoctorWorkingDay {
  dayOfWeek: number;
  working: boolean;
  startTime: string | null;
  endTime: string | null;
}

export interface DoctorWorkingHours {
  doctorUserId: number;
  doctorName: string;
  days: DoctorWorkingDay[];
}

export interface Material {
  id: number;
  name: string;
  category: string;
  quantity: number;
  unit: string;
  minimumStock: number;
  expirationDate: string;
  supplier: string;
  status: 'Available' | 'Low Stock' | 'Out of Stock';
  batchNumber?: string;
  lastUsed?: string;
  monthlyConsumption?: number;
}

export interface WorkingDay {
  day: string;
  opensAt: string;
  closesAt: string;
  isWorking: boolean;
}

export interface TreatmentSummary {
  treatment: string;
  count: number;
  revenue: number;
  color: string;
}

export interface OfficeExpense {
  id: number;
  label: string;
  category: 'Internet' | 'Maintenance' | 'Materials' | 'Electricity' | 'Water' | 'Patente';
  amount: number;
  dueDate: string;
  status: 'Paid' | 'Pending' | 'Scheduled';
  owner: string;
}

export interface Secretary {
  id: number;
  fullName: string;
  role: string;
  avatar: string;
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  time: string;
  read: boolean;
}

export type PaymentMethod = 'Cash' | 'Card' | 'Bank Transfer';
export type PaymentStatus = 'Full' | 'Partial';

export interface PatientPayment {
  id: number;
  patientId: number;
  patientName: string;
  treatment: string;
  treatmentCost: number;
  totalPaid: number;
  receivedAmount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  date: string;
  receipt: string;
}

export interface ClinicExpense {
  id: number;
  date: string;
  category:
    | 'Dental Materials'
    | 'Laboratory'
    | 'Maintenance'
    | 'Electricity'
    | 'Internet'
    | 'Water'
    | 'Salaries'
    | 'Taxes'
    | 'Cleaning'
    | 'Equipment'
    | 'Marketing'
    | 'Miscellaneous';
  amount: number;
  description: string;
  supplier: string;
  invoiceNumber?: string;
  unexpectedNote?: string;
}

export type ClinicAvailabilityStatus = 'Working Day' | 'Holiday' | 'Vacation' | 'Closed' | 'Emergency Only';

export interface ClinicAvailability {
  id: number;
  date: string;
  status: ClinicAvailabilityStatus;
  note: string;
}

export interface DoctorInstruction {
  id: number;
  patientName: string;
  text: string;
  priority: 'Routine' | 'Important' | 'Urgent';
  due: string;
}

export interface TreatmentStep {
  id: number;
  patientId: number;
  label: string;
  status: 'Pending' | 'Scheduled' | 'In Progress' | 'Completed';
  date: string;
  notes: string;
}

export interface TreatmentCost {
  patientId: number;
  totalPrice: number;
  sessions: number;
  discount: number;
  insuranceCoverage: number;
  finalBalance: number;
}

export interface ClinicalNote {
  id: number;
  patientId: number;
  patientName: string;
  diagnosis: string;
  observations: string;
  prescription: string;
  recommendations: string;
  futurePlan: string;
  date: string;
}
