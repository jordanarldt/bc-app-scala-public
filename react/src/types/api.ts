
export interface PermissionsResponse {
  role: string;
}

export interface StoreUser {
  userId: number;
  email: string;
  role: string;
  lastLogin: number;
}

export interface PaginationInfo {
  currentPage: number;
  totalPages: number;
  totalItems: number;
}

export interface Variant {
  id: number;
  product_id: number;
  product_name: string;
  sku: string;
  inventory_tracking: string;
  inventory_level: number;
}

export interface BcMeta {
  pagination: {
    total: number;
    count: number;
    per_page: number;
    current_page: number;
    total_pages: number;
  }
}
