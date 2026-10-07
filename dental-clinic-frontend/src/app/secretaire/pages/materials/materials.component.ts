import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { materials as mockMaterials } from '../../data/mock-secretary.data';
import {
  InventoryMaterial,
  MaterialPayload,
  MaterialStatus,
} from '../../../shared/models/material.models';
import { MaterialService } from '../../../shared/services/material.service';
import { ModalComponent } from '../../shared/modal/modal.component';

@Component({
  selector: 'app-secretary-materials',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './materials.component.html',
  styleUrl: './materials.component.css',
})
export class MaterialsComponent implements OnInit {
  private readonly materialService = inject(MaterialService);
  private readonly platformId = inject(PLATFORM_ID);

  materials: InventoryMaterial[] = [];
  query = '';
  filter: MaterialStatus | '' = '';
  categoryFilter = '';
  sortBy = 'name';
  selectedMaterial?: InventoryMaterial;
  modalOpen = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  formModel: MaterialPayload = this.emptyForm();

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.materials = this.fallbackMaterials();
      return;
    }
    this.loadMaterials();
  }

  get categories(): string[] {
    return [...new Set(this.materials.map((material) => material.category))];
  }

  get filteredMaterials(): InventoryMaterial[] {
    const value = this.query.toLowerCase();
    return [...this.materials]
      .filter((material) => !this.filter || material.status === this.filter)
      .filter((material) => !this.categoryFilter || material.category === this.categoryFilter)
      .filter((material) =>
        `${material.name} ${material.category} ${material.supplier ?? ''}`
          .toLowerCase()
          .includes(value),
      )
      .sort((a, b) =>
        this.sortBy === 'qty'
          ? a.quantity - b.quantity
          : this.sortBy === 'exp'
            ? (a.expirationDate ?? '').localeCompare(b.expirationDate ?? '')
            : a.name.localeCompare(b.name),
      );
  }

  stockPercent(material: InventoryMaterial): number {
    return Math.min(
      100,
      Math.round((material.quantity / Math.max(material.minimumStock * 2, 1)) * 100),
    );
  }

  openMaterial(material?: InventoryMaterial): void {
    this.selectedMaterial = material;
    this.errorMessage = '';
    this.successMessage = '';
    this.formModel = material
      ? {
          name: material.name,
          category: material.category,
          quantity: material.quantity,
          unit: material.unit,
          minimumStock: material.minimumStock,
          expirationDate: material.expirationDate ?? null,
          supplier: material.supplier ?? null,
          status: material.status,
          batchNumber: material.batchNumber ?? null,
          monthlyConsumption: material.monthlyConsumption,
          purchaseCost: material.purchaseCost,
        }
      : this.emptyForm();
    this.modalOpen = true;
  }

  saveMaterial(): void {
    if (this.saving || !this.formValid()) return;
    this.saving = true;
    this.errorMessage = '';
    const request = this.selectedMaterial
      ? this.materialService.updateMaterial(this.selectedMaterial.id, this.formModel)
      : this.materialService.createMaterial(this.formModel);
    const creating = !this.selectedMaterial;
    request.subscribe({
      next: (material) => {
        this.materials = this.materials.some((item) => item.id === material.id)
          ? this.materials.map((item) => (item.id === material.id ? material : item))
          : [material, ...this.materials];
        this.saving = false;
        this.modalOpen = false;
        this.successMessage = creating
          ? `Material saved. A ${this.currency(material.purchaseCost)} expense was created automatically.`
          : 'Material updated. No duplicate expense was created.';
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Unable to save the material.';
      },
    });
  }

  countByStatus(status: MaterialStatus): number {
    return this.materials.filter((material) => material.status === status).length;
  }

  statusLabel(status: MaterialStatus): string {
    return status
      .toLowerCase()
      .replaceAll('_', ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private loadMaterials(): void {
    this.materialService.getMaterials().subscribe({
      next: (materials) => (this.materials = materials),
      error: () => {
        this.materials = this.fallbackMaterials();
        this.errorMessage = 'Unable to load saved materials. Showing fallback inventory.';
      },
    });
  }

  private formValid(): boolean {
    if (
      !this.formModel.name.trim() ||
      this.formModel.quantity < 0 ||
      this.formModel.minimumStock < 0 ||
      !this.formModel.expirationDate ||
      this.formModel.purchaseCost <= 0
    ) {
      this.errorMessage =
        'Complete name, quantity, minimum stock, expiration date, and purchase cost.';
      return false;
    }
    return true;
  }

  private emptyForm(): MaterialPayload {
    return {
      name: '',
      category: '',
      quantity: 0,
      unit: '',
      minimumStock: 0,
      expirationDate: null,
      supplier: null,
      status: 'AVAILABLE',
      batchNumber: null,
      monthlyConsumption: 0,
      purchaseCost: 0,
    };
  }

  private currency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }

  private fallbackMaterials(): InventoryMaterial[] {
    return mockMaterials.map((material) => ({
      id: material.id,
      name: material.name,
      category: material.category,
      quantity: material.quantity,
      unit: material.unit,
      minimumStock: material.minimumStock,
      expirationDate: material.expirationDate,
      supplier: material.supplier,
      status:
        material.status === 'Available'
          ? 'AVAILABLE'
          : material.status === 'Low Stock'
            ? 'LOW_STOCK'
            : 'OUT_OF_STOCK',
      batchNumber: material.batchNumber ?? null,
      monthlyConsumption: material.monthlyConsumption ?? 0,
      purchaseCost: 0,
      purchaseExpenseId: null,
      createdAt: material.expirationDate,
      updatedAt: material.expirationDate,
    }));
  }
}
