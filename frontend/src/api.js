const TOKEN_KEY = 'makon_access_token'

export const authStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

export async function api(path, options = {}) {
  const token = authStore.get()
  const headers = new Headers(options.headers || {})
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...options, headers })
  const contentType = response.headers.get('content-type') || ''
  const payload = response.status === 204
    ? null
    : contentType.includes('application/json')
      ? await response.json()
      : await response.text()

  if (!response.ok) {
    if (response.status === 401) authStore.clear()
    const validationMessage = Array.isArray(payload?.validationErrors)
      ? payload.validationErrors.map(error => error.message).filter(Boolean).join(', ')
      : ''
    const message = validationMessage || payload?.message || payload || `HTTP ${response.status}`
    throw new ApiError(message, response.status, payload)
  }
  return payload
}

export const endpoints = {
  register: (body) => api('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => api('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  profile: () => api('/api/v1/users/me'),
  categories: () => api('/api/v1/categories'),
  createCategory: (body) => api('/api/v1/categories', { method: 'POST', body: JSON.stringify(body) }),
  products: ({ page = 0, size = 24, search, categoryId, minPrice, maxPrice, sort = 'createdAt,desc' } = {}) => {
    const params = new URLSearchParams({ page, size, sort })
    if (search?.trim()) params.set('search', search.trim())
    if (categoryId && categoryId !== 'all') params.set('categoryId', categoryId)
    if (minPrice !== '' && minPrice != null) params.set('minPrice', minPrice)
    if (maxPrice !== '' && maxPrice != null) params.set('maxPrice', maxPrice)
    return api(`/api/v1/products?${params}`)
  },
  myProducts: () => api('/api/v1/products/me?size=50&sort=createdAt,desc'),
  createProduct: (body) => api('/api/v1/products', { method: 'POST', body: JSON.stringify(body) }),
  productAction: (id, action) => api(`/api/v1/products/me/${id}/${action}`, { method: 'PATCH' }),
  myProductImages: (productId) => api(`/api/v1/products/me/${productId}/images`),
  uploadProductImage: (productId, file) => {
    const body = new FormData()
    body.append('file', file)
    return api(`/api/v1/products/me/${productId}/images`, { method: 'POST', body })
  },
  setPrimaryProductImage: (productId, imageId) => api(
    `/api/v1/products/me/${productId}/images/${imageId}/primary`,
    { method: 'PATCH' },
  ),
  reorderProductImages: (productId, imageIds) => api(
    `/api/v1/products/me/${productId}/images/order`,
    { method: 'PUT', body: JSON.stringify({ imageIds }) },
  ),
  deleteProductImage: (productId, imageId) => api(
    `/api/v1/products/me/${productId}/images/${imageId}`,
    { method: 'DELETE' },
  ),
  publicProductImages: (storeSlug, productSlug) => api(
    `/api/v1/stores/${encodeURIComponent(storeSlug)}/products/${encodeURIComponent(productSlug)}/images`,
  ),
  storeProducts: (storeSlug, page = 0, size = 24, sort = 'createdAt,desc') => {
    const params = new URLSearchParams({ page, size, sort })
    return api(`/api/v1/stores/${encodeURIComponent(storeSlug)}/products?${params}`)
  },
  sellerProfile: () => api('/api/v1/sellers/me'),
  applySeller: () => api('/api/v1/sellers/apply', { method: 'POST' }),
  pendingSellers: () => api('/api/v1/sellers/pending'),
  moderateSeller: (id, action, reason) => api(`/api/v1/sellers/${id}/${action}`, {
    method: 'PATCH',
    body: action === 'reject' ? JSON.stringify({ rejectionReason: reason || 'Заявка требует уточнения данных' }) : undefined,
  }),
  myStore: () => api('/api/v1/stores/me'),
  createStore: (body) => api('/api/v1/stores', { method: 'POST', body: JSON.stringify(body) }),
  updateStore: (body) => api('/api/v1/stores/me', { method: 'PUT', body: JSON.stringify(body) }),
  toggleStore: (action) => api(`/api/v1/stores/me/${action}`, { method: 'PATCH' }),
  updateProduct: (id, body) => api(`/api/v1/products/me/${id}`, {
    method: 'PUT', body: JSON.stringify(body),
  }),
  inventory: (productId) => api(`/api/v1/inventory/products/${productId}`),
  addStock: (productId, amount) => api(`/api/v1/inventory/products/${productId}/add-stock`, {
    method: 'POST', body: JSON.stringify({ amount }),
  }),
  removeStock: (productId, amount) => api(`/api/v1/inventory/products/${productId}/remove-stock`, {
    method: 'POST', body: JSON.stringify({ amount }),
  }),
  cart: () => api('/api/v1/cart'),
  addCartItem: (productId, quantity = 1) => api('/api/v1/cart/items', {
    method: 'POST', body: JSON.stringify({ productId, quantity }),
  }),
  updateCartItem: (itemId, quantity) => api(`/api/v1/cart/items/${itemId}`, {
    method: 'PATCH', body: JSON.stringify({ quantity }),
  }),
  removeCartItem: (itemId) => api(`/api/v1/cart/items/${itemId}`, { method: 'DELETE' }),
  orders: ({ page = 0, size = 20, sort = 'createdAt,desc' } = {}) => {
    const params = new URLSearchParams({ page, size, sort })
    return api(`/api/v1/orders?${params}`)
  },
  order: (orderId) => api(`/api/v1/orders/${orderId}`),
  createOrder: () => api('/api/v1/orders', { method: 'POST' }),
  cancelOrder: (orderId) => api(`/api/v1/orders/${orderId}/cancel`, { method: 'PATCH' }),
}
