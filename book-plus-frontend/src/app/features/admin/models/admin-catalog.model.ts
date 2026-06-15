/** Mirrors catalog-service CreateBookRequest / UpdateBookRequest. */
export interface BookFormValue {
  isbn: string;
  title: string;
  author: string;
  description: string;
  price: number;
  currency: string;
  imageUrl: string;
  previewUrl: string;
  publisher: string;
  publishedDate: string | null;
  language: string;
  pages: number | null;
  categoryId: string;
}

export type CreateBookRequest = BookFormValue;
export type UpdateBookRequest = BookFormValue;

export interface CreateCategoryRequest {
  name: string;
}
