import { appointments, clients, materials, officeExpenses, treatmentSummary } from '../secretaire/data/mock-secretary.data';
import { Material } from '../secretaire/models/secretary.models';

export const doctorAppointments = appointments;
export const doctorClients = clients;
export const doctorMaterials = materials;
export const doctorExpenses = officeExpenses;
export const doctorTreatments = treatmentSummary;

export const todaysDoctorAppointments = doctorAppointments.filter((appointment) => appointment.date === '2026-08-04');
export const lowDoctorMaterials = doctorMaterials.filter((material) => material.status !== 'Available');

export const totalRevenue = doctorTreatments.reduce((total, treatment) => total + treatment.revenue, 0);
export const totalExpenses = doctorExpenses.reduce((total, expense) => total + expense.amount, 0);
export const netIncome = totalRevenue - totalExpenses;
export const workHours = Math.round(doctorAppointments.reduce((total, appointment) => total + appointment.duration, 0) / 60);
export const patientBalance = doctorClients.reduce((total, client) => total + (client.billingBalance ?? 0), 0);
export const patientLifetimeValue = doctorClients.reduce((total, client) => total + (client.lifetimeValue ?? 0), 0);

export const doctorStats = [
  { label: 'Visites du jour', value: `${todaysDoctorAppointments.length}`, hint: 'Rendez-vous du 4 aout' },
  { label: 'Heures reservees', value: `${workHours}h`, hint: 'Charge du planning' },
  { label: 'Revenu brut', value: `$${formatShort(totalRevenue)}`, hint: 'Revenus des soins' },
  { label: 'Depenses', value: `$${formatShort(totalExpenses)}`, hint: 'Factures du cabinet' },
  { label: 'Revenu net', value: `$${formatShort(netIncome)}`, hint: 'Revenus moins factures' },
  { label: 'Croissance', value: '+14.8%', hint: 'Tendance mensuelle' },
];

export function stockPercent(material: Material): number {
  return Math.min(100, Math.round((material.quantity / Math.max(material.minimumStock * 2, 1)) * 100));
}

export function formatShort(value: number): string {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(value >= 10000 ? 1 : 2)}k`;
  }
  return `${value}`;
}

export function labelFr(value: string | undefined): string {
  const labels: Record<string, string> = {
    Confirmed: 'Confirme',
    Waiting: 'En attente',
    'In progress': 'En cours',
    Completed: 'Termine',
    Cancelled: 'Annule',
    Routine: 'Routine',
    Urgent: 'Urgent',
    Critical: 'Critique',
    Active: 'Actif',
    New: 'Nouveau',
    'Follow-up': 'Suivi',
    Paused: 'En pause',
    Available: 'Disponible',
    'Low Stock': 'Stock faible',
    'Out of Stock': 'Rupture',
    Paid: 'Paye',
    PAID: 'Paye',
    Pending: 'En attente',
    PENDING: 'En attente',
    Scheduled: 'Planifie',
    SCHEDULED: 'Planifie',
    CANCELLED: 'Annule',
  };
  return value ? labels[value] ?? value : '';
}
