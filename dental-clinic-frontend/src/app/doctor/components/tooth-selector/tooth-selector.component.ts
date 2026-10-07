import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TreatmentProcedure } from '../../models/treatment.models';

type ToothVisual = 'healthy' | 'selected' | 'completed' | 'in-progress' | 'planned' | 'missing' | 'implant' | 'crown';

@Component({
  selector: 'app-tooth-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tooth-selector.component.html',
  styleUrl: './tooth-selector.component.css',
})
export class ToothSelectorComponent {
  @Input() selectedTeeth: number[] = [];
  @Input() procedures: TreatmentProcedure[] = [];
  @Input() allTeeth = false;
  @Output() selectedTeethChange = new EventEmitter<number[]>();
  @Output() allTeethChange = new EventEmitter<boolean>();

  upper = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
  lower = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];

  toggle(tooth: number): void {
    if (this.allTeeth) {
      this.allTeeth = false;
      this.allTeethChange.emit(false);
    }
    const set = new Set(this.selectedTeeth);
    set.has(tooth) ? set.delete(tooth) : set.add(tooth);
    this.selectedTeethChange.emit([...set].sort((a, b) => a - b));
  }

  toggleAll(): void {
    this.allTeeth = !this.allTeeth;
    this.allTeethChange.emit(this.allTeeth);
    this.selectedTeethChange.emit([]);
  }

  clear(): void {
    this.allTeeth = false;
    this.allTeethChange.emit(false);
    this.selectedTeethChange.emit([]);
  }

  procedureCount(tooth: number): number {
    return this.procedures.filter((item) => item.toothNumber === tooth).length;
  }

  visualFor(tooth: number): ToothVisual {
    if (this.allTeeth || this.selectedTeeth.includes(tooth)) return 'selected';
    const related = this.procedures.filter((item) => item.toothNumber === tooth);
    if (related.some((item) => item.name.toLowerCase().includes('implant'))) return 'implant';
    if (related.some((item) => item.name.toLowerCase().includes('crown'))) return 'crown';
    if (related.some((item) => item.name.toLowerCase().includes('extraction'))) return 'missing';
    if (related.some((item) => item.status === 'COMPLETED')) return 'completed';
    if (related.some((item) => item.status === 'IN_PROGRESS')) return 'in-progress';
    if (related.some((item) => item.status === 'PLANNED')) return 'planned';
    return 'healthy';
  }
}
