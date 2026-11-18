export const demoCategories = [
  { id: 'c1', name: 'Электроника', slug: 'electronics', isActive: true, parentId: null, icon: '⌁' },
  { id: 'c2', name: 'Для дома', slug: 'home', isActive: true, parentId: null, icon: '⌂' },
  { id: 'c3', name: 'Красота', slug: 'beauty', isActive: true, parentId: null, icon: '✦' },
  { id: 'c4', name: 'Спорт', slug: 'sport', isActive: true, parentId: null, icon: '◉' },
  { id: 'c5', name: 'Аксессуары', slug: 'accessories', isActive: true, parentId: null, icon: '◇' },
]

export const demoProducts = [
  { id: 'p1', storeId: 's1', categoryId: 'c1', name: 'Nova X1', slug: 'nova-x1', description: 'Флагманский смартфон с ярким OLED-дисплеем и умной камерой.', status: 'PUBLISHED', accent: 'violet', price: '6 490 000 сум', tag: 'Хит' },
  { id: 'p2', storeId: 's1', categoryId: 'c1', name: 'Airbuds Flow', slug: 'airbuds-flow', description: 'Лёгкие наушники с активным шумоподавлением на весь день.', status: 'PUBLISHED', accent: 'mint', price: '749 000 сум', tag: 'Новинка' },
  { id: 'p3', storeId: 's2', categoryId: 'c2', name: 'Luma Desk', slug: 'luma-desk', description: 'Минималистичная настольная лампа с мягкой регулировкой света.', status: 'PUBLISHED', accent: 'orange', price: '389 000 сум', tag: '−15%' },
  { id: 'p4', storeId: 's3', categoryId: 'c3', name: 'Sahar No. 04', slug: 'sahar-no-04', description: 'Тёплый древесный аромат с нотами инжира и белого чая.', status: 'PUBLISHED', accent: 'rose', price: '529 000 сум', tag: 'Выбор' },
  { id: 'p5', storeId: 's2', categoryId: 'c2', name: 'Mizu Set', slug: 'mizu-set', description: 'Набор из четырёх керамических чашек ручной формы.', status: 'PUBLISHED', accent: 'sky', price: '269 000 сум', tag: 'Локально' },
  { id: 'p6', storeId: 's4', categoryId: 'c4', name: 'Move Bottle', slug: 'move-bottle', description: 'Термобутылка из стали для тренировок и поездок.', status: 'PUBLISHED', accent: 'lime', price: '199 000 сум', tag: 'Bestseller' },
]

export const demoAccounts = {
  customer: { username: 'demo-customer', password: 'Customer123!', role: 'CUSTOMER' },
  seller: { username: 'demo-seller', password: 'Seller123!', role: 'SELLER' },
  admin: { username: 'demo-admin', password: 'Admin123!', role: 'ADMIN' },
}

export const demoWorkspace = {
  profile: { id: 'u-demo', username: 'demo-seller', status: 'ACTIVE' },
  seller: { id: 'seller-demo', userId: 'u-demo', status: 'APPROVED', approvedAt: '2026-11-03T09:00:00', rejectionReason: null },
  store: { id: 'store-demo', sellerId: 'seller-demo', name: 'Nova Market', slug: 'nova-market', description: 'Технологии, которыми приятно пользоваться каждый день.', active: true },
}
