export interface Stock {
  id: string;
  bookId: string;
  quantityTotal: number;
  quantityAvailable: number;
  quantityReserved: number;
  lowStockThreshold: number;
  inStock: boolean;
  lowStock: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** Mirrors inventory-service AdjustStockRequest (PUT /inventory/{bookId}/adjust). */
export interface AdjustStockRequest {
  newTotalQuantity: number;
  lowStockThreshold: number;
  notes?: string;
}
