export type MaterialStatus = 'AVAILABLE' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export interface InventoryMaterial {
  id: number;
  name: string;
  category: string;
  quantity: number;
  unit: string;
  minimumStock: number;
  expirationDate?: string | null;
  supplier?: string | null;
  status: MaterialStatus;
  batchNumber?: string | null;
  monthlyConsumption: number;
  purchaseCost: number;
  purchaseExpenseId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export type MaterialPayload = Omit<
  InventoryMaterial,
  'id' | 'purchaseExpenseId' | 'createdAt' | 'updatedAt'
>;
