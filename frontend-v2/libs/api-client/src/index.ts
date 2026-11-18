export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
};

export type Category = {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  isActive: boolean;
  parentId?: string | null;
};

export type ProductStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type Product = {
  id: string;
  storeId: string;
  storeSlug: string;
  categoryId: string;
  name: string;
  slug: string;
  description?: string | null;
  status: ProductStatus;
  moderationBlocked?: boolean;
  createdAt: string;
  updatedAt: string;
  price: number;
  images?: ProductImage[];
  primaryImageUrl?: string;
};

export type ProductImage = {
  id: string;
  productId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  url: string;
  primary: boolean;
  sortOrder: number;
  createdAt: string;
};
export type ProductVariant = { id: string; productId: string; sku: string; name: string; price: number; active: boolean; quantity: number; reservedQuantity: number; availableQuantity: number };
export type Review = { id: string; productId: string; username: string; rating: number; comment?: string | null; status: string; createdAt: string };
export type ProductRating = { productId: string; count: number; averageRating: number };

export type UserProfile = { id: string; username: string; status: string; fullName?: string | null; phone?: string | null };
export type Seller = { id: string; userId: string; status: string; approvedAt?: string | null; rejectionReason?: string | null };
export type Store = { id: string; sellerId: string; name: string; slug: string; description?: string | null; active: boolean; approved: boolean; rejectionReason?: string | null };
export type PlatformStatistics = { users: number; sellers: number; stores: number; products: number; orders: number; confirmedOrders: number; reviews: number };
export type AuditLog = { id: string; actor: string; action: string; targetType: string; targetId: string; details?: string | null; createdAt: string };
export type Promocode = { id: string; code: string; discountType: 'PERCENT' | 'FIXED'; discountValue: number; minimumAmount?: number | null; startsAt?: string | null; expiresAt?: string | null; maxUses?: number | null; usedCount: number; active: boolean };
export type Inventory = { productId: string; quantity: number; reservedQuantity: number; availableQuantity: number };
export type CartItem = { itemId: string; productId: string; variantId?: string | null; variantName?: string | null; productName: string; unitPrice: number; quantity: number; subtotal: number };
export type Cart = { cartId: string | null; items: CartItem[]; totalItems: number; totalPrice: number };
export type OrderItem = { itemId: string; productId: string; variantId?: string | null; sku?: string | null; productName: string; quantity: number; unitPrice: number; subtotal: number };
export type DeliveryDetails = { recipientName: string; recipientPhone: string; region: string; city: string; addressLine: string };
export type DeliveryRegion = { code: string; name: string };
export type SellerOrder = {
  sellerOrderId: string;
  orderId: string;
  storeId: string;
  status: string;
  items: OrderItem[];
  totalAmount: number;
  confirmedAt?: string | null;
  cancelledAt?: string | null;
  cancellationReson?: string | null;
  delivery?: DeliveryDetails | null;
};
export type Order = { orderId: string; status: string; sellerOrders: SellerOrder[]; totalAmount: number; subtotalAmount: number; discountAmount: number; promocode?: string | null; delivery?: DeliveryDetails | null; createdAt: string };

export const emptyCart: Cart = { cartId: null, items: [], totalItems: 0, totalPrice: 0 };
const TOKEN_KEY = 'makon_access_token';
const REFRESH_KEY = 'makon_refresh_token';

export const authStore = {
  get: () => typeof window === 'undefined' ? null : window.localStorage.getItem(TOKEN_KEY),
  getRefresh: () => typeof window === 'undefined' ? null : window.localStorage.getItem(REFRESH_KEY),
  set: (token: string) => { if (typeof window !== 'undefined') window.localStorage.setItem(TOKEN_KEY, token); },
  setTokens: (accessToken: string, refreshToken: string) => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(TOKEN_KEY, accessToken);
      window.localStorage.setItem(REFRESH_KEY, refreshToken);
    }
  },
  clear: () => {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(TOKEN_KEY);
      window.localStorage.removeItem(REFRESH_KEY);
    }
  },
};

type Session = { accessToken: string; tokenType: string; refreshToken: string };
let refreshPromise: Promise<boolean> | null = null;
async function refreshSession(): Promise<boolean> {
  const refreshToken = authStore.getRefresh();
  if (!refreshToken) return false;
  try {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) { authStore.clear(); return false; }
    const session = await response.json() as Session;
    authStore.setTokens(session.accessToken, session.refreshToken);
    return true;
  } catch { return false; }
}

export class ApiError extends Error {
  constructor(message: string, public status: number, public payload: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

type RequestOptions = Omit<RequestInit, 'body'> & { body?: unknown };

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const token = authStore.get();
  const isFormData = options.body instanceof FormData;
  if (options.body != null && !isFormData) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const body: BodyInit | undefined = options.body == null
    ? undefined
    : isFormData
      ? options.body as FormData
      : JSON.stringify(options.body);
  let response = await fetch(path, {
    ...options,
    headers,
    body,
  });
  if (response.status === 401 && !path.startsWith('/api/v1/auth/') && authStore.getRefresh()) {
    refreshPromise ??= refreshSession().finally(() => { refreshPromise = null; });
    if (await refreshPromise) {
      headers.set('Authorization', `Bearer ${authStore.get()}`);
      response = await fetch(path, { ...options, headers, body });
    }
  }
  const contentType = response.headers.get('content-type') ?? '';
  const payload = response.status === 204 ? null : contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    if (response.status === 401) authStore.clear();
    const objectPayload = payload && typeof payload === 'object' ? payload as Record<string, unknown> : null;
    const validationErrors = Array.isArray(objectPayload?.validationErrors) ? objectPayload.validationErrors as Array<Record<string, unknown>> : [];
    const validationMessage = validationErrors.map((entry) => entry.message).filter(Boolean).join(', ');
    const message = validationMessage || String(objectPayload?.message ?? payload ?? `HTTP ${response.status}`);
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

function params(values: Record<string, string | number | null | undefined>) {
  const result = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value != null) result.set(key, String(value));
  });
  return result.toString();
}

export const api = {
  login: (body: { username: string; password: string }) => request<Session>('/api/v1/auth/login', { method: 'POST', body }),
  logout: () => { const refreshToken = authStore.getRefresh(); return refreshToken ? request<void>('/api/v1/auth/logout', { method: 'POST', body: { refreshToken } }) : Promise.resolve(); },
  register: (body: { username: string; password: string }) => request<unknown>('/api/v1/auth/register', { method: 'POST', body }),
  profile: () => request<UserProfile>('/api/v1/users/me'),
  updateProfile: (body: { fullName?: string; phone?: string }) => request<UserProfile>('/api/v1/users/me', { method: 'PUT', body }),
  favorites: () => request<PageResponse<{ id: string; product: Product; createdAt: string }>>('/api/v1/favorites?size=100'),
  addFavorite: (productId: string) => request<unknown>(`/api/v1/favorites/${productId}`, { method: 'POST' }),
  removeFavorite: (productId: string) => request<void>(`/api/v1/favorites/${productId}`, { method: 'DELETE' }),
  categories: () => request<Category[]>('/api/v1/categories'),
  deliveryRegions: () => request<DeliveryRegion[]>('/api/v1/delivery/regions'),
  createCategory: (body: { name: string; slug: string; description?: string; parentId?: string | null }) => request<Category>('/api/v1/categories', { method: 'POST', body }),
  updateCategory: (id: string, body: { name: string; slug: string; description?: string; active: boolean; parentId?: string | null }) => request<Category>(`/api/v1/categories/${id}`, { method: 'PUT', body }),
  deactivateCategory: (id: string) => request<void>(`/api/v1/categories/${id}`, { method: 'DELETE' }),
  products: (filters: { page?: number; size?: number; search?: string; categoryId?: string; minPrice?: string | number; maxPrice?: string | number; storeSlug?: string; availableOnly?: boolean; sort?: string } = {}) => request<PageResponse<Product>>(`/api/v1/products?${params({ page: filters.page ?? 0, size: filters.size ?? 24, search: filters.search?.trim(), categoryId: filters.categoryId === 'all' ? undefined : filters.categoryId, minPrice: filters.minPrice, maxPrice: filters.maxPrice, storeSlug: filters.storeSlug === 'all' ? undefined : filters.storeSlug, availableOnly: filters.availableOnly ? 'true' : undefined, sort: filters.sort ?? 'createdAt,desc' })}`),
  stores: () => request<PageResponse<Store>>('/api/v1/stores?size=100&sort=name,asc'),
  publicProductImages: (storeSlug: string, productSlug: string) => request<ProductImage[]>(`/api/v1/stores/${encodeURIComponent(storeSlug)}/products/${encodeURIComponent(productSlug)}/images`),
  storeProducts: (storeSlug: string, page = 0, size = 24) => request<PageResponse<Product>>(`/api/v1/stores/${encodeURIComponent(storeSlug)}/products?${params({ page, size, sort: 'createdAt,desc' })}`),
  cart: () => request<Cart>('/api/v1/cart'),
  addCartItem: (productId: string, quantity = 1, variantId?: string) => request<Cart>('/api/v1/cart/items', { method: 'POST', body: { productId, quantity, variantId } }),
  updateCartItem: (itemId: string, quantity: number) => request<Cart>(`/api/v1/cart/items/${itemId}`, { method: 'PATCH', body: { quantity } }),
  removeCartItem: (itemId: string) => request<Cart>(`/api/v1/cart/items/${itemId}`, { method: 'DELETE' }),
  orders: (page = 0, size = 20) => request<PageResponse<Order>>(`/api/v1/orders?${params({ page, size, sort: 'createdAt,desc' })}`),
  order: (orderId: string) => request<Order>(`/api/v1/orders/${orderId}`),
  createOrder: (idempotencyKey: string, delivery: DeliveryDetails, promocode?: string) => request<Order>('/api/v1/orders', { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey }, body: { promocode, delivery } }),
  previewPromocode: (code: string, subtotal: number) => request<{ code: string; subtotalAmount: number; discountAmount: number; totalAmount: number }>(`/api/v1/promocodes/preview?${params({ code, subtotal })}`),
  orderHistory: (orderId: string) => request<Array<{ id: string; sellerOrderId?: string; previousStatus?: string; newStatus: string; actor: string; reason?: string; createdAt: string }>>(`/api/v1/orders/${orderId}/history`),
  cancelOrder: (orderId: string) => request<Order>(`/api/v1/orders/${orderId}/cancel`, { method: 'PATCH' }),
  sellerOrders: (page = 0, size = 20) => request<PageResponse<SellerOrder>>(`/api/v1/seller/orders?${params({ page, size, sort: 'createdAt,desc' })}`),
  confirmSellerOrder: (id: string) => request<SellerOrder>(`/api/v1/seller/orders/${id}/confirm`, { method: 'PATCH' }),
  cancelSellerOrder: (id: string, reason: string) => request<SellerOrder>(`/api/v1/seller/orders/${id}/cancel`, { method: 'PATCH', body: { reason } }),
  sellerProfile: () => request<Seller>('/api/v1/sellers/me'),
  applySeller: () => request<Seller>('/api/v1/sellers/apply', { method: 'POST' }),
  pendingSellers: () => request<Seller[]>('/api/v1/sellers/pending'),
  moderateSeller: (id: string, action: 'approve' | 'reject', rejectionReason?: string) => request<Seller>(`/api/v1/sellers/${id}/${action}`, { method: 'PATCH', body: action === 'reject' ? { rejectionReason: rejectionReason || 'Заявка требует уточнения данных' } : undefined }),
  adminStatistics: () => request<PlatformStatistics>('/api/v1/admin/statistics'),
  adminUsers: () => request<PageResponse<UserProfile>>('/api/v1/admin/users?size=100'),
  adminUserStatus: (id: string, action: 'block' | 'unblock') => request<UserProfile>(`/api/v1/admin/users/${id}/${action}`, { method: 'PATCH' }),
  adminPendingStores: () => request<PageResponse<Store>>('/api/v1/admin/stores/pending?size=100'),
  adminModerateStore: (id: string, action: 'approve' | 'reject', reason?: string) => request<Store>(`/api/v1/admin/stores/${id}/${action}`, { method: 'PATCH', body: action === 'reject' ? { reason } : undefined }),
  adminProducts: () => request<PageResponse<Product>>('/api/v1/admin/products?size=100'),
  adminModerateProduct: (id: string, action: 'block' | 'unblock', reason?: string) => request<Product>(`/api/v1/admin/products/${id}/${action}`, { method: 'PATCH', body: action === 'block' ? { reason } : undefined }),
  adminAudit: () => request<PageResponse<AuditLog>>('/api/v1/admin/audit?size=100'),
  adminPendingReviews: () => request<PageResponse<Review>>('/api/v1/reviews/pending?size=100'),
  adminModerateReview: (id: string, action: 'approve' | 'reject') => request<Review>(`/api/v1/reviews/${id}/${action}`, { method: 'PATCH' }),
  adminPromocodes: () => request<PageResponse<Promocode>>('/api/v1/admin/promocodes?size=100'),
  adminCreatePromocode: (body: { code: string; discountType: 'PERCENT' | 'FIXED'; discountValue: number; minimumAmount?: number; startsAt?: string; expiresAt?: string; maxUses?: number }) => request<Promocode>('/api/v1/admin/promocodes', { method: 'POST', body }),
  adminDeactivatePromocode: (id: string) => request<Promocode>(`/api/v1/admin/promocodes/${id}/deactivate`, { method: 'PATCH' }),
  myStore: () => request<Store>('/api/v1/stores/me'),
  createStore: (body: { name: string; slug: string; description?: string }) => request<Store>('/api/v1/stores', { method: 'POST', body }),
  updateStore: (body: { name: string; slug: string; description?: string }) => request<Store>('/api/v1/stores/me', { method: 'PUT', body }),
  toggleStore: (action: 'activate' | 'deactivate') => request<Store>(`/api/v1/stores/me/${action}`, { method: 'PATCH' }),
  myProducts: () => request<PageResponse<Product>>('/api/v1/products/me?size=100&sort=createdAt,desc'),
  createProduct: (body: { categoryId: string; name: string; slug: string; description?: string; price: number }) => request<Product>('/api/v1/products', { method: 'POST', body }),
  updateProduct: (id: string, body: { categoryId: string; name: string; slug: string; description?: string; price: number }) => request<Product>(`/api/v1/products/me/${id}`, { method: 'PUT', body }),
  productAction: (id: string, action: 'publish' | 'archive' | 'restore') => request<Product>(`/api/v1/products/me/${id}/${action}`, { method: 'PATCH' }),
  publicVariants: (productId: string) => request<ProductVariant[]>(`/api/v1/products/${productId}/variants`),
  productReviews: (productId: string) => request<PageResponse<Review>>(`/api/v1/reviews/products/${productId}?size=20`),
  productRating: (productId: string) => request<ProductRating>(`/api/v1/reviews/products/${productId}/rating`),
  createReview: (productId: string, rating: number, comment: string) => request<Review>('/api/v1/reviews', { method: 'POST', body: { productId, rating, comment } }),
  myVariants: (productId: string) => request<ProductVariant[]>(`/api/v1/products/me/${productId}/variants`),
  createVariant: (productId: string, body: { name: string; sku: string; price: number }) => request<ProductVariant>(`/api/v1/products/me/${productId}/variants`, { method: 'POST', body }),
  updateVariant: (productId: string, variantId: string, body: { name: string; price: number; active: boolean }) => request<ProductVariant>(`/api/v1/products/me/${productId}/variants/${variantId}`, { method: 'PUT', body }),
  changeVariantStock: (productId: string, variantId: string, kind: 'add' | 'remove', amount: number) => request<ProductVariant>(`/api/v1/products/me/${productId}/variants/${variantId}/${kind}-stock`, { method: 'POST', body: { amount } }),
  inventory: (productId: string) => request<Inventory>(`/api/v1/inventory/products/${productId}`),
  addStock: (productId: string, amount: number) => request<Inventory>(`/api/v1/inventory/products/${productId}/add-stock`, { method: 'POST', body: { amount } }),
  removeStock: (productId: string, amount: number) => request<Inventory>(`/api/v1/inventory/products/${productId}/remove-stock`, { method: 'POST', body: { amount } }),
  myProductImages: (productId: string) => request<ProductImage[]>(`/api/v1/products/me/${productId}/images`),
  uploadProductImage: (productId: string, file: File) => { const body = new FormData(); body.append('file', file); return request<ProductImage>(`/api/v1/products/me/${productId}/images`, { method: 'POST', body }); },
  setPrimaryProductImage: (productId: string, imageId: string) => request<ProductImage>(`/api/v1/products/me/${productId}/images/${imageId}/primary`, { method: 'PATCH' }),
  deleteProductImage: (productId: string, imageId: string) => request<void>(`/api/v1/products/me/${productId}/images/${imageId}`, { method: 'DELETE' }),
};

export function formatPrice(value: number | string | null | undefined) {
  if (value == null || value === '') return 'Цена не указана';
  const number = Number(value);
  if (!Number.isFinite(number)) return String(value);
  return `${new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(number)} сум`;
}
