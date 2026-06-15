export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  parentId?: string;
  imageUrl?: string;
  active?: boolean;
}

/** Lightweight projection used in listings and search (BookSummaryResponse). */
export interface BookSummary {
  id: string;
  isbn: string;
  title: string;
  slug: string;
  author: string;
  imageUrl?: string;
  price: number;
  currency: string;
  discountPrice?: number;
  hasDiscount: boolean;
  inStock: boolean;
  averageRating?: number;
  reviewCount: number;
  categoryId: string;
}

/** Full detail projection (BookResponse). */
export interface Book {
  id: string;
  isbn: string;
  title: string;
  slug: string;
  author: string;
  description: string;
  price: number;
  currency: string;
  discountPrice?: number;
  hasDiscount: boolean;
  imageUrl?: string;
  publisher?: string;
  publishedDate?: string;
  language?: string;
  pages?: number;
  categoryId: string;
  active: boolean;
  inStock: boolean;
  stockSnapshot: number;
  averageRating?: number;
  reviewCount: number;
  /** Optional sample/preview document URL (PDF) when the backend provides it. */
  previewUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Review {
  id: string;
  bookId: string;
  userId: string;
  username?: string;
  rating: number;
  comment: string;
  verifiedPurchase: boolean;
  createdAt: string;
}

export interface BookBrowseParams {
  q?: string;
  categoryId?: string;
  author?: string;
  page?: number;
  size?: number;
}
