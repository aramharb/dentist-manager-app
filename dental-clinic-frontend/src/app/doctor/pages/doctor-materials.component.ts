import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { doctorMaterials, labelFr, lowDoctorMaterials, stockPercent } from '../doctor-dashboard.data';
import { SessionService } from '../../core/auth/session.service';

@Component({
  selector: 'app-doctor-materials',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="dashboard-grid">
      <article class="panel">
        <div class="section-heading"><h2>Material consumption</h2><span>{{ filteredMaterials.length }} articles</span></div>
        <input class="search-input" [(ngModel)]="search" placeholder="Rechercher materiel, categorie, fournisseur, lot" />
        <label>Select material<select [(ngModel)]="selectedId"><option *ngFor="let material of materials" [ngValue]="material.id">{{ material.name }}</option></select></label>
        <label>Quantity used<input type="number" [(ngModel)]="usedQuantity" /></label>
        <div class="cost-card">
          <span>Current stock</span><strong>{{ selected.quantity }} {{ selected.unit }}</strong>
          <small>Used Today: {{ usedQuantity }} · Remaining: {{ remaining }} {{ selected.unit }}</small>
        </div>
        <p class="warning" *ngIf="remaining <= selected.minimumStock">Low stock warning: reorder {{ selected.name }}.</p>
        <div class="row-card" *ngFor="let material of filteredMaterials">
          <div>
            <strong>{{ material.name }}</strong>
            <p>Current Stock: {{ material.quantity }} {{ material.unit }}</p>
            <small>Used Today: {{ material.monthlyConsumption ? (material.monthlyConsumption / 10 | number:'1.0-0') : 0 }} · Remaining: {{ material.quantity }} · Low limit {{ material.minimumStock }}</small>
            <div class="progress"><i [style.width.%]="stockPercent(material)"></i></div>
          </div>
          <span class="pill">{{ labelFr(material.status) }}</span>
        </div>
      </article>
      <article class="panel">
        <div class="owner-mini" *ngIf="currentUser$ | async as user"><div><strong>{{ user.fullName }}</strong><small>Doctor workspace</small></div></div>
        <div class="section-heading"><h2>Statistiques du stock</h2><span>{{ lowMaterials.length }} alertes</span></div>
        <div class="row-card"><div><strong>{{ monthlyUse }} unites utilisees</strong><p>Consommation mensuelle estimee.</p></div></div>
        <div class="row-card"><div><strong>{{ reorderCount }} articles a commander</strong><p>Materiels faibles ou en rupture.</p></div></div>
        <div class="action-row"><button type="button">Demander un achat</button><button type="button" class="ghost-button">Marquer utilise</button><button type="button" class="ghost-button">Fournisseurs</button></div>
        <div class="table material-table">
          <div class="table-head"><span>Material</span><span>Stock</span><span>Used</span><span>Remaining</span><span>Unit</span></div>
          <div class="table-row" *ngFor="let material of materials"><strong>{{ material.name }}</strong><span>{{ material.quantity }}</span><span>{{ material.monthlyConsumption ? (material.monthlyConsumption / 10 | number:'1.0-0') : 0 }}</span><span>{{ material.quantity }}</span><span>{{ material.unit }}</span></div>
        </div>
      </article>
    </section>
  `,
  styleUrl: './doctor-page.css',
})
export class DoctorMaterialsComponent {
  readonly currentUser$ = inject(SessionService).currentUser$;
  materials = doctorMaterials;
  lowMaterials = lowDoctorMaterials;
  stockPercent = stockPercent;
  labelFr = labelFr;
  search = '';
  selectedId = 1;
  usedQuantity = 3;

  get filteredMaterials() {
    const value = this.search.trim().toLowerCase();
    if (!value) return this.materials;
    return this.materials.filter((material) => `${material.name} ${material.category} ${material.supplier} ${material.batchNumber}`.toLowerCase().includes(value));
  }

  get monthlyUse(): number {
    return this.materials.reduce((total, material) => total + (material.monthlyConsumption ?? 0), 0);
  }

  get reorderCount(): number {
    return this.lowMaterials.length;
  }

  get selected() {
    return this.materials.find((material) => material.id === this.selectedId) ?? this.materials[0];
  }

  get remaining(): number {
    return Math.max(0, this.selected.quantity - Number(this.usedQuantity || 0));
  }
}
