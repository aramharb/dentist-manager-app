import {
  Appointment,
  Client,
  Material,
  Notification,
  OfficeExpense,
  ClinicAvailability,
  ClinicExpense,
  ClinicalNote,
  DoctorInstruction,
  Secretary,
  PatientPayment,
  TreatmentCost,
  TreatmentStep,
  TreatmentSummary,
  WorkingDay,
} from '../models/secretary.models';

export const secretary: Secretary = {
  id: 1,
  fullName: 'Nour Belkacem',
  role: 'Secretaire medicale',
  avatar: 'NB',
};

export const clients: Client[] = [
  {
    id: 1,
    firstName: 'Ahmed',
    lastName: 'Mansouri',
    birthDate: '1988-04-12',
    age: 38,
    gender: 'Male',
    contact: { phone: '+213 555 012 884', email: 'ahmed.m@email.com', address: 'Hydra, Alger', emergencyContact: '+213 555 777 142' },
    bloodType: 'O+',
    allergies: 'Penicillin',
    medicalHistory: 'Mild hypertension',
    currentMedications: 'Amlodipine',
    assignedDentist: 'Dr. Wajih',
    lastAppointment: '2026-08-01',
    nextAppointment: '2026-08-04',
    status: 'Follow-up',
    mainComplaint: 'Sensitivity after filling',
    treatmentProgress: 68,
    notes: 'Prefers morning appointments.',
    avatar: 'AM',
    insuranceProvider: 'CNAS Premium',
    billingBalance: 240,
    lifetimeValue: 2840,
    documents: 6,
  },
  {
    id: 2,
    firstName: 'Sarah',
    lastName: 'Benali',
    birthDate: '1994-11-03',
    age: 31,
    gender: 'Female',
    contact: { phone: '+213 555 420 100', email: 'sarah.b@email.com', address: 'Kouba, Alger', emergencyContact: '+213 555 420 101' },
    bloodType: 'A+',
    allergies: 'None',
    medicalHistory: 'No chronic conditions',
    currentMedications: 'None',
    assignedDentist: 'Dr. Lina Merabet',
    lastAppointment: '2026-07-28',
    nextAppointment: '2026-08-04',
    status: 'Active',
    mainComplaint: 'Orthodontic review',
    treatmentProgress: 42,
    notes: 'Clear aligner check every 3 weeks.',
    avatar: 'SB',
    insuranceProvider: 'Private Care Plus',
    billingBalance: 0,
    lifetimeValue: 3910,
    documents: 9,
  },
  {
    id: 3,
    firstName: 'Ali',
    lastName: 'Rahmani',
    birthDate: '1979-02-18',
    age: 47,
    gender: 'Male',
    contact: { phone: '+213 555 876 930', email: 'ali.r@email.com', address: 'Bab Ezzouar, Alger', emergencyContact: '+213 555 111 930' },
    bloodType: 'B-',
    allergies: 'Latex',
    medicalHistory: 'Diabetes type 2',
    currentMedications: 'Metformin',
    assignedDentist: 'Dr. Wajih',
    lastAppointment: '2026-07-12',
    nextAppointment: '2026-08-04',
    status: 'Active',
    mainComplaint: 'Crown preparation',
    treatmentProgress: 75,
    notes: 'Check blood sugar before long procedures.',
    avatar: 'AR',
    insuranceProvider: 'CNAS',
    billingBalance: 680,
    lifetimeValue: 4620,
    documents: 12,
  },
  {
    id: 4,
    firstName: 'Meriem',
    lastName: 'Saadi',
    birthDate: '2001-09-20',
    age: 24,
    gender: 'Female',
    contact: { phone: '+213 555 333 674', email: 'meriem.s@email.com', address: 'El Biar, Alger', emergencyContact: '+213 555 333 675' },
    bloodType: 'AB+',
    allergies: 'Ibuprofen',
    medicalHistory: 'Asthma',
    currentMedications: 'Salbutamol',
    assignedDentist: 'Dr. Lina Merabet',
    lastAppointment: '2026-06-26',
    nextAppointment: '2026-08-08',
    status: 'New',
    mainComplaint: 'Whitening consultation',
    treatmentProgress: 12,
    notes: 'New patient onboarding complete.',
    avatar: 'MS',
    insuranceProvider: 'Self pay',
    billingBalance: 120,
    lifetimeValue: 420,
    documents: 2,
  },
];

export const appointments: Appointment[] = [
  { id: 1, clientId: 1, clientName: 'Ahmed Mansouri', date: '2026-08-04', time: '09:00', duration: 45, treatmentType: 'Consultation', status: 'Confirmed', dentist: 'Dr. Wajih', color: '#1d9bf0', notes: 'Tooth 26 follow-up', priority: 'Routine', room: 'Room 2', photo: 'AM' },
  { id: 2, clientId: 2, clientName: 'Sarah Benali', date: '2026-08-04', time: '10:00', duration: 30, treatmentType: 'Orthodontics', status: 'Waiting', dentist: 'Dr. Lina Merabet', color: '#8b5cf6', priority: 'Routine', room: 'Ortho bay', photo: 'SB' },
  { id: 3, clientId: 3, clientName: 'Ali Rahmani', date: '2026-08-04', time: '11:30', duration: 60, treatmentType: 'Surgery', status: 'Confirmed', dentist: 'Dr. Wajih', color: '#f97316', priority: 'Urgent', room: 'Surgery 1', photo: 'AR' },
  { id: 4, clientId: 4, clientName: 'Meriem Saadi', date: '2026-08-08', time: '14:00', duration: 40, treatmentType: 'Cleaning', status: 'Confirmed', dentist: 'Dr. Lina Merabet', color: '#22c55e', priority: 'Routine', room: 'Room 1', photo: 'MS' },
  { id: 5, clientId: 2, clientName: 'Sarah Benali', date: '2026-08-11', time: '09:30', duration: 30, treatmentType: 'Emergency', status: 'Confirmed', dentist: 'Dr. Lina Merabet', color: '#ef4444', priority: 'Critical', room: 'Room 3', photo: 'SB' },
];

export const materials: Material[] = [
  { id: 1, name: 'Composite Resin A2', category: 'Restorative', quantity: 18, unit: 'syringes', minimumStock: 8, expirationDate: '2027-02-10', supplier: 'DentPlus', status: 'Available', batchNumber: 'DP-A2-771', lastUsed: 'Today', monthlyConsumption: 21 },
  { id: 2, name: 'Nitrile Gloves M', category: 'Protection', quantity: 6, unit: 'boxes', minimumStock: 10, expirationDate: '2028-05-02', supplier: 'MedSupply', status: 'Low Stock', batchNumber: 'MS-GM-204', lastUsed: 'Today', monthlyConsumption: 34 },
  { id: 3, name: 'Anesthetic Carpules', category: 'Medication', quantity: 0, unit: 'packs', minimumStock: 5, expirationDate: '2026-12-21', supplier: 'PharmaDent', status: 'Out of Stock', batchNumber: 'PD-AN-087', lastUsed: 'Yesterday', monthlyConsumption: 15 },
  { id: 4, name: 'Impression Material', category: 'Prosthetics', quantity: 12, unit: 'kits', minimumStock: 4, expirationDate: '2027-01-15', supplier: 'OrthoLine', status: 'Available', batchNumber: 'OL-IM-552', lastUsed: 'Mon', monthlyConsumption: 8 },
  { id: 5, name: 'Sterilization Pouches', category: 'Sterilization', quantity: 9, unit: 'packs', minimumStock: 12, expirationDate: '2029-08-19', supplier: 'CleanCare', status: 'Low Stock', batchNumber: 'CC-SP-919', lastUsed: 'Today', monthlyConsumption: 29 },
];

export const workingDays: WorkingDay[] = [
  { day: 'Monday', opensAt: '08:00', closesAt: '17:00', isWorking: true },
  { day: 'Tuesday', opensAt: '08:00', closesAt: '17:00', isWorking: true },
  { day: 'Wednesday', opensAt: '08:00', closesAt: '17:00', isWorking: true },
  { day: 'Thursday', opensAt: '08:00', closesAt: '17:00', isWorking: true },
  { day: 'Friday', opensAt: '08:00', closesAt: '15:00', isWorking: true },
  { day: 'Saturday', opensAt: '09:00', closesAt: '13:00', isWorking: false },
  { day: 'Sunday', opensAt: 'Closed', closesAt: 'Closed', isWorking: false },
];

export const treatmentSummary: TreatmentSummary[] = [
  { treatment: 'Restorative', count: 38, revenue: 9200, color: '#1d9bf0' },
  { treatment: 'Orthodontics', count: 22, revenue: 14800, color: '#06b6d4' },
  { treatment: 'Prosthetics', count: 16, revenue: 12450, color: '#22c55e' },
  { treatment: 'Whitening', count: 11, revenue: 3650, color: '#f59e0b' },
];

export const notifications: Notification[] = [
  { id: 1, title: 'Low stock', message: 'Nitrile Gloves M below minimum stock.', time: '08:30', read: false },
  { id: 2, title: 'Appointment soon', message: 'Sarah Benali starts in 15 minutes.', time: '09:45', read: false },
  { id: 3, title: 'New client', message: 'Meriem Saadi completed intake form.', time: 'Yesterday', read: true },
  { id: 4, title: 'Patient arrived', message: 'Ahmed Mansouri checked in at reception.', time: '08:54', read: false },
  { id: 5, title: 'Birthday reminder', message: 'Send a birthday note to Ali Rahmani this week.', time: 'Mon', read: true },
  { id: 6, title: 'Payment received', message: 'Ahmed Mansouri paid $600 by card.', time: '10:25', read: false },
  { id: 7, title: 'Doctor note', message: 'Prepare implant kit for Ali Rahmani.', time: '11:05', read: false },
  { id: 8, title: 'Holiday scheduled', message: 'Clinic marked closed on 2026-08-20.', time: 'Yesterday', read: true },
];

export const officeExpenses: OfficeExpense[] = [
  { id: 1, label: 'Fiber internet connection', category: 'Internet', amount: 85, dueDate: '2026-08-06', status: 'Scheduled', owner: 'Reception' },
  { id: 2, label: 'Chair compressor maintenance', category: 'Maintenance', amount: 420, dueDate: '2026-08-09', status: 'Pending', owner: 'Operations' },
  { id: 3, label: 'Composite and sterilization materials buy', category: 'Materials', amount: 960, dueDate: '2026-08-12', status: 'Pending', owner: 'Inventory' },
  { id: 4, label: 'Electricity bill', category: 'Electricity', amount: 310, dueDate: '2026-08-15', status: 'Scheduled', owner: 'Administration' },
  { id: 5, label: 'Water bill', category: 'Water', amount: 74, dueDate: '2026-08-18', status: 'Scheduled', owner: 'Administration' },
  { id: 6, label: 'Patente tax provision', category: 'Patente', amount: 640, dueDate: '2026-08-25', status: 'Pending', owner: 'Administration' },
];

export const treatmentCosts: TreatmentCost[] = [
  { patientId: 1, totalPrice: 1800, sessions: 4, discount: 120, insuranceCoverage: 540, finalBalance: 1140 },
  { patientId: 2, totalPrice: 3200, sessions: 10, discount: 250, insuranceCoverage: 900, finalBalance: 2050 },
  { patientId: 3, totalPrice: 2600, sessions: 5, discount: 100, insuranceCoverage: 780, finalBalance: 1720 },
  { patientId: 4, totalPrice: 650, sessions: 2, discount: 0, insuranceCoverage: 0, finalBalance: 650 },
];

export const patientPayments: PatientPayment[] = [
  { id: 1, patientId: 1, patientName: 'Ahmed Mansouri', treatment: 'Filling and crown review', treatmentCost: 1140, totalPaid: 900, receivedAmount: 600, method: 'Card', status: 'Partial', date: '2026-08-04', receipt: 'RCT-1048' },
  { id: 2, patientId: 2, patientName: 'Sarah Benali', treatment: 'Orthodontic aligners', treatmentCost: 2050, totalPaid: 2050, receivedAmount: 850, method: 'Bank Transfer', status: 'Full', date: '2026-08-03', receipt: 'RCT-1047' },
  { id: 3, patientId: 3, patientName: 'Ali Rahmani', treatment: 'Crown preparation', treatmentCost: 1720, totalPaid: 1040, receivedAmount: 300, method: 'Cash', status: 'Partial', date: '2026-08-02', receipt: 'RCT-1046' },
  { id: 4, patientId: 4, patientName: 'Meriem Saadi', treatment: 'Whitening consultation', treatmentCost: 650, totalPaid: 120, receivedAmount: 120, method: 'Cash', status: 'Partial', date: '2026-08-01', receipt: 'RCT-1045' },
];

export const clinicExpenses: ClinicExpense[] = [
  { id: 1, date: '2026-08-04', category: 'Dental Materials', amount: 960, description: 'Composite resin and polishing discs', supplier: 'DentPlus', invoiceNumber: 'DP-7781' },
  { id: 2, date: '2026-08-03', category: 'Laboratory', amount: 420, description: 'Zirconia crown fabrication', supplier: 'SmileLab', invoiceNumber: 'SL-2104' },
  { id: 3, date: '2026-08-02', category: 'Maintenance', amount: 310, description: 'Compressor preventive service', supplier: 'MedTech Service', invoiceNumber: 'MT-662' },
  { id: 4, date: '2026-08-01', category: 'Electricity', amount: 275, description: 'Monthly energy bill', supplier: 'Utility Office' },
  { id: 5, date: '2026-07-30', category: 'Miscellaneous', amount: 95, description: 'Urgent courier for surgical guide', supplier: 'City Express', unexpectedNote: 'Other / Unexpected Expense: same-day implant guide delivery' },
];

export const clinicAvailability: ClinicAvailability[] = [
  { id: 1, date: '2026-08-05', status: 'Working Day', note: 'Full schedule' },
  { id: 2, date: '2026-08-09', status: 'Emergency Only', note: 'Doctor on call' },
  { id: 3, date: '2026-08-15', status: 'Vacation', note: 'Team retreat' },
  { id: 4, date: '2026-08-20', status: 'Closed', note: 'Equipment calibration' },
  { id: 5, date: '2026-09-01', status: 'Holiday', note: 'Public holiday' },
];

export const doctorInstructions: DoctorInstruction[] = [
  { id: 1, patientName: 'Ahmed Mansouri', text: 'Collect remaining balance after crown review.', priority: 'Important', due: 'Today' },
  { id: 2, patientName: 'Sarah Benali', text: 'Ask patient to confirm next appointment.', priority: 'Routine', due: 'Tomorrow' },
  { id: 3, patientName: 'Ali Rahmani', text: 'Prepare implant kit and contact insurance.', priority: 'Urgent', due: 'Today' },
  { id: 4, patientName: 'Meriem Saadi', text: 'Send whitening consent form before visit.', priority: 'Routine', due: 'This week' },
];

export const treatmentSteps: TreatmentStep[] = [
  { id: 1, patientId: 1, label: 'Consultation', status: 'Completed', date: '2026-07-18', notes: 'Initial sensitivity assessment' },
  { id: 2, patientId: 1, label: 'X-Ray', status: 'Completed', date: '2026-07-18', notes: 'Bitewing reviewed' },
  { id: 3, patientId: 1, label: 'Filling', status: 'Completed', date: '2026-07-25', notes: 'Tooth 26 restored' },
  { id: 4, patientId: 1, label: 'Crown', status: 'In Progress', date: '2026-08-08', notes: 'Shade selection pending' },
  { id: 5, patientId: 1, label: 'Final Check', status: 'Scheduled', date: '2026-08-18', notes: 'Occlusion review' },
  { id: 6, patientId: 3, label: 'Implant', status: 'Scheduled', date: '2026-08-12', notes: 'Surgical guide requested' },
];

export const clinicalNotes: ClinicalNote[] = [
  { id: 1, patientId: 1, patientName: 'Ahmed Mansouri', diagnosis: 'Post-restoration sensitivity', observations: 'Mild cold sensitivity, no spontaneous pain.', prescription: 'Desensitizing toothpaste twice daily.', recommendations: 'Avoid hard chewing on left side for 48 hours.', futurePlan: 'Crown review and final check after bite adjustment.', date: '2026-08-04' },
  { id: 2, patientId: 3, patientName: 'Ali Rahmani', diagnosis: 'Missing molar with controlled diabetes', observations: 'Gingiva healthy, blood sugar check required before surgery.', prescription: 'Chlorhexidine rinse for 5 days before implant.', recommendations: 'Coordinate insurance authorization.', futurePlan: 'Implant placement then crown after osseointegration.', date: '2026-08-02' },
];
