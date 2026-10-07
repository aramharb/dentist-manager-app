import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { appointments, materials, officeExpenses } from '../../data/mock-secretary.data';

@Component({
  selector: 'app-secretary-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  todaysAppointments = appointments.filter((appointment) => appointment.date === '2026-08-04');
  lowStockMaterials = materials.filter((material) => material.status !== 'Available');
  todayClients = new Set(this.todaysAppointments.map((appointment) => appointment.clientId)).size;
  lowStock = this.lowStockMaterials.length;
  officeExpenses = officeExpenses;
  daySlots = ['09:00', '10:00', '11:00', '11:30', '13:00', '14:00', '15:30'];

  appointmentAt(slot: string) {
    return this.todaysAppointments.find((appointment) => appointment.time === slot);
  }
}
