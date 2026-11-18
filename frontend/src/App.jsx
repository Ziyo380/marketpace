import { useEffect, useMemo, useState } from 'react'
import {
  ArrowRight, BadgeCheck, Boxes, ChevronRight, CircleUserRound, Flame,
  ChevronDown, ChevronUp, ImagePlus, LayoutDashboard, LogOut, Menu,
  Minus, PackageCheck, Plus, Search, ShieldCheck, ShoppingBag, ShoppingCart,
  Sparkles, Star, Store, Trash2, UploadCloud, UserRoundCheck, X, Zap,
} from 'lucide-react'
import { ApiError, authStore, endpoints } from './api.js'

const accentCycle = ['violet', 'mint', 'orange', 'rose', 'sky', 'lime']
const FAVORITES_KEY = 'makon_favorite_product_ids'
const emptyCart = { cartId: null, items: [], totalItems: 0, totalPrice: 0 }
const productImageBySlug = {
  'nova-x1': '/products/electronics.png',
  'airbuds-flow': '/products/audio.png',
  'luma-desk': '/products/home.png',
  'sahar-no-04': '/products/beauty.png',
  'move-bottle': '/products/sport.png',
  'orbit-watch-s': '/products/watch.png',
  'studio-headphones': '/products/audio.png',
  'mizu-ceramic-set': '/products/ceramic.png',
  'aura-skin-serum': '/products/beauty.png',
  'flex-yoga-mat': '/products/sport.png',
  'halo-desk-lamp': '/products/home.png',
  'pulse-mini-speaker': '/products/audio.png',
}
const productImageByCategory = {
  '2ac43520-5c23-4bb9-8dd3-388b18bcd6d1': '/products/electronics.png',
  '6e91fc90-dcfd-4345-80d0-48b0bc607e9d': '/products/electronics.png',
  '40000000-0000-0000-0000-000000000002': '/products/home.png',
  '40000000-0000-0000-0000-000000000003': '/products/beauty.png',
  '40000000-0000-0000-0000-000000000004': '/products/sport.png',
}

function localProductImage(product) {
  return productImageBySlug[product.slug] || productImageByCategory[product.categoryId] || '/products/electronics.png'
}

function loadFavoriteIds() {
  try {
    const value = JSON.parse(localStorage.getItem(FAVORITES_KEY) || '[]')
    return new Set(Array.isArray(value) ? value : [])
  } catch {
    return new Set()
  }
}

function normalizePage(payload) {
  if (Array.isArray(payload)) return payload
  return payload?.content || []
}

function formatPrice(value) {
  if (value == null || value === '') return 'Цена не указана'
  if (typeof value === 'string' && value.toLowerCase().includes('сум')) return value
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return String(value)
  return `${new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(numericValue)} сум`
}

function numericPrice(value) {
  if (typeof value === 'number') return value
  const normalized = String(value ?? '').replace(/[^\d,.-]/g, '').replace(',', '.')
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

function App() {
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [backend, setBackend] = useState('checking')
  const [catalogError, setCatalogError] = useState('')
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState('all')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [sort, setSort] = useState('createdAt,desc')
  const [authOpen, setAuthOpen] = useState(false)
  const [workspaceOpen, setWorkspaceOpen] = useState(false)
  const [session, setSession] = useState(null)
  const [toast, setToast] = useState('')
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [favoriteIds, setFavoriteIds] = useState(loadFavoriteIds)
  const [cart, setCart] = useState(emptyCart)
  const [cartOpen, setCartOpen] = useState(false)
  const [cartBusy, setCartBusy] = useState(false)

  useEffect(() => {
    loadCatalog()
    if (authStore.get()) restoreSession()
  }, [])

  useEffect(() => {
    if (session && authStore.get()) refreshCart()
    if (!session) setCart(emptyCart)
  }, [session])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(''), 3500)
    return () => clearTimeout(timer)
  }, [toast])

  async function loadCatalog(filters = {}) {
    const activeFilters = {
      search: filters.search ?? query,
      categoryId: filters.categoryId ?? categoryId,
      minPrice: filters.minPrice ?? minPrice,
      maxPrice: filters.maxPrice ?? maxPrice,
      sort: filters.sort ?? sort,
    }
    try {
      setCatalogLoading(true)
      setCatalogError('')
      const [categoryPayload, productPayload] = await Promise.all([
        endpoints.categories(),
        endpoints.products(activeFilters),
      ])
      const catalogProducts = await Promise.all(normalizePage(productPayload).map(async product => {
        if (!product.storeSlug) return product
        try {
          const images = await endpoints.publicProductImages(product.storeSlug, product.slug)
          const primaryImage = images.find(image => image.primary) || images[0]
          return { ...product, images, primaryImageUrl: primaryImage?.url || localProductImage(product) }
        } catch {
          return { ...product, images: [], primaryImageUrl: localProductImage(product) }
        }
      }))
      setCategories(categoryPayload)
      setProducts(catalogProducts)
      setBackend('online')
    } catch (error) {
      setBackend('offline')
      setCatalogError(error instanceof ApiError ? error.message : 'Не удалось подключиться к backend')
    } finally {
      setCatalogLoading(false)
    }
  }

  async function restoreSession() {
    try {
      const profile = await endpoints.profile()
      setSession({ profile, role: 'USER' })
    } catch {
      authStore.clear()
    }
  }

  async function refreshCart() {
    try {
      setCart(await endpoints.cart())
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) return
      setToast(error.message || 'Не удалось загрузить корзину')
    }
  }

  async function addToCart(product, quantity = 1) {
    if (!session) {
      setAuthOpen(true)
      setToast('Сначала войдите в аккаунт')
      return false
    }
    setCartBusy(true)
    try {
      setCart(await endpoints.addCartItem(product.id, quantity))
      setToast(`${product.name} добавлен в корзину`)
      return true
    } catch (error) {
      setToast(error.message || 'Не удалось добавить товар')
      return false
    } finally {
      setCartBusy(false)
    }
  }

  const filteredProducts = useMemo(() => {
    const filtered = products.filter((product) => {
      const matchesCategory = categoryId === 'all' || product.categoryId === categoryId
      const searchText = `${product.name} ${product.description || ''}`.toLowerCase()
      const productPrice = numericPrice(product.price)
      const matchesMin = minPrice === '' || productPrice >= Number(minPrice)
      const matchesMax = maxPrice === '' || productPrice <= Number(maxPrice)
      return matchesCategory && matchesMin && matchesMax && searchText.includes(query.trim().toLowerCase())
    })
    const [field, direction] = sort.split(',')
    return [...filtered].sort((left, right) => {
      let comparison = 0
      if (field === 'price') comparison = numericPrice(left.price) - numericPrice(right.price)
      if (field === 'name') comparison = left.name.localeCompare(right.name, 'ru')
      if (field === 'createdAt') comparison = String(left.createdAt || '').localeCompare(String(right.createdAt || ''))
      return direction === 'desc' ? -comparison : comparison
    })
  }, [products, query, categoryId, minPrice, maxPrice, sort])

  function selectCategory(nextCategoryId) {
    setCategoryId(nextCategoryId)
    loadCatalog({ categoryId: nextCategoryId })
  }

  function applyCatalogFilters() {
    loadCatalog()
    document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' })
  }

  function resetCatalogFilters() {
    setQuery('')
    setCategoryId('all')
    setMinPrice('')
    setMaxPrice('')
    setSort('createdAt,desc')
    loadCatalog({ search: '', categoryId: 'all', minPrice: '', maxPrice: '', sort: 'createdAt,desc' })
  }

  function logout() {
    authStore.clear()
    setSession(null)
    setWorkspaceOpen(false)
    setToast('Вы вышли из аккаунта')
  }

  function toggleFavorite(product) {
    setFavoriteIds(current => {
      const next = new Set(current)
      const added = !next.has(product.id)
      if (added) next.add(product.id)
      else next.delete(product.id)
      localStorage.setItem(FAVORITES_KEY, JSON.stringify([...next]))
      setToast(added ? 'Товар добавлен в избранное' : 'Товар удалён из избранного')
      return next
    })
  }

  return (
    <div className="app-shell">
      <Header
        backend={backend}
        session={session}
        cartCount={cart.totalItems || 0}
        onAuth={() => setAuthOpen(true)}
        onWorkspace={() => setWorkspaceOpen(true)}
        onCart={() => session ? setCartOpen(true) : setAuthOpen(true)}
      />

      <main>
        <section className="hero section-wrap">
          <div className="hero-copy">
            <div className="eyebrow"><Sparkles size={15} /> Новый взгляд на привычные покупки</div>
            <h1>Всё нужное.<br/><span>Ближе, чем кажется.</span></h1>
            <p>Открывайте локальные бренды и технологичные магазины в одном спокойном, понятном пространстве.</p>
            <div className="hero-search">
              <Search size={20} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && applyCatalogFilters()}
                placeholder="Что вы ищете сегодня?"
              />
              <button onClick={applyCatalogFilters}>
                Найти <ArrowRight size={18} />
              </button>
            </div>
            <div className="hero-proof">
              <div><strong>120+</strong><span>локальных магазинов</span></div>
              <div><strong>24 ч</strong><span>на обработку заказа</span></div>
              <div><strong>4.9</strong><span>средняя оценка</span></div>
            </div>
          </div>

          <div className="hero-art" aria-label="Подборка популярных товаров">
            <div className="orbit orbit-one" />
            <div className="orbit orbit-two" />
            <div className="floating-card card-a"><span>⌁</span><b>Nova X1</b><small>умнее каждый день</small></div>
            <div className="floating-card card-b"><Flame size={18}/><b>Выбор недели</b><small>24 новых бренда</small></div>
            <div className="floating-card card-c"><span>✦</span><b>Sahar No. 04</b><small>тёплая коллекция</small></div>
            <div className="hero-badge"><BadgeCheck size={18}/><span><b>Проверенные</b> продавцы</span></div>
          </div>
        </section>

        {backend === 'offline' && (
          <div className="demo-banner section-wrap">
            <span><Zap size={18}/> Backend недоступен: {catalogError || 'проверьте запуск Spring Boot'}.</span>
            <button onClick={loadCatalog}>Проверить снова</button>
          </div>
        )}

        <section className="categories section-wrap" id="categories">
          <div className="section-heading compact">
            <div><span className="section-kicker">Быстрый старт</span><h2>Категории</h2></div>
            <button className="text-button" onClick={() => selectCategory('all')}>Смотреть все <ChevronRight size={17}/></button>
          </div>
          <div className="category-row">
            <button className={`category-pill ${categoryId === 'all' ? 'active' : ''}`} onClick={() => selectCategory('all')}>
              <span>∞</span><b>Всё</b>
            </button>
            {categories.slice(0, 7).map((category, index) => (
              <button
                key={category.id}
                className={`category-pill ${categoryId === category.id ? 'active' : ''}`}
                onClick={() => selectCategory(category.id)}
              >
                <span>{category.icon || ['⌁','⌂','✦','◉','◇','○'][index % 6]}</span>
                <b>{category.name}</b>
              </button>
            ))}
          </div>
        </section>

        <section className="catalog section-wrap" id="catalog">
          <div className="section-heading">
            <div>
              <span className="section-kicker">Сейчас выбирают</span>
              <h2>{query ? `Результаты для «${query}»` : 'Витрина недели'}</h2>
              <p>{catalogLoading ? 'Загружаем товары…' : `${filteredProducts.length} товаров в подборке`}</p>
            </div>
            <div className="catalog-note"><PackageCheck size={19}/><span>Статусы товаров приходят напрямую из backend</span></div>
          </div>

          <div className="catalog-filters">
            <label>Цена от<input type="number" min="0" step="0.01" value={minPrice} onChange={event => setMinPrice(event.target.value)} placeholder="0"/></label>
            <label>Цена до<input type="number" min="0" step="0.01" value={maxPrice} onChange={event => setMaxPrice(event.target.value)} placeholder="Без ограничения"/></label>
            <label>Сортировка<select value={sort} onChange={event => { setSort(event.target.value); loadCatalog({ sort: event.target.value }) }}>
              <option value="createdAt,desc">Сначала новые</option>
              <option value="price,asc">Сначала дешевле</option>
              <option value="price,desc">Сначала дороже</option>
              <option value="name,asc">По названию</option>
            </select></label>
            <button className="primary-action" onClick={applyCatalogFilters}>Применить</button>
            <button className="secondary-action" onClick={resetCatalogFilters}>Сбросить</button>
          </div>

          <div className="product-grid">
            {catalogLoading && <CatalogSkeleton />}
            {!catalogLoading && filteredProducts.map((product, index) => (
              <ProductCard
                key={product.id}
                product={product}
                index={index}
                favorite={favoriteIds.has(product.id)}
                onFavorite={() => toggleFavorite(product)}
                onOpen={() => setSelectedProduct(product)}
              />
            ))}
            {!catalogLoading && !filteredProducts.length && (
              <div className="empty-state">
                <Search size={30}/><h3>Ничего не нашли</h3><p>Попробуйте изменить запрос или выбрать другую категорию.</p>
                <button onClick={resetCatalogFilters}>Сбросить фильтры</button>
              </div>
            )}
          </div>
        </section>

        <section className="seller-cta section-wrap">
          <div>
            <span className="section-kicker light">Для предпринимателей</span>
            <h2>Ваш магазин заслуживает<br/>свою аудиторию.</h2>
            <p>Подайте заявку продавца, создайте витрину и управляйте публикациями из одного кабинета.</p>
          </div>
          <button onClick={() => session ? setWorkspaceOpen(true) : setAuthOpen(true)}>
            Открыть кабинет <ArrowRight size={19}/>
          </button>
        </section>
      </main>

      <Footer />

      {selectedProduct && (
        <ProductDetailModal
          product={selectedProduct}
          favorite={favoriteIds.has(selectedProduct.id)}
          onFavorite={() => toggleFavorite(selectedProduct)}
          onAddCart={(quantity) => addToCart(selectedProduct, quantity)}
          cartBusy={cartBusy}
          onClose={() => setSelectedProduct(null)}
        />
      )}

      {cartOpen && (
        <CartDrawer
          cart={cart}
          busy={cartBusy}
          onClose={() => setCartOpen(false)}
          onCart={setCart}
          onBusy={setCartBusy}
          onToast={setToast}
          onOrderCreated={() => { setCart(emptyCart); setCartOpen(false); setWorkspaceOpen(true) }}
        />
      )}

      {authOpen && (
        <AuthModal
          onClose={() => setAuthOpen(false)}
          onSession={(nextSession) => { setSession(nextSession); setAuthOpen(false); setWorkspaceOpen(true) }}
          onToast={setToast}
        />
      )}

      {workspaceOpen && session && (
        <Workspace
          session={session}
          categories={categories}
          onClose={() => setWorkspaceOpen(false)}
          onLogout={logout}
          onToast={setToast}
          onCatalogRefresh={loadCatalog}
        />
      )}

      {toast && <div className="toast"><BadgeCheck size={18}/>{toast}</div>}
    </div>
  )
}

function Header({ backend, session, cartCount, onAuth, onWorkspace, onCart }) {
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <header className="site-header">
      <div className="header-inner section-wrap">
        <a className="brand" href="#"><span className="brand-mark">M</span><span>makon<small>market</small></span></a>
        <nav className={menuOpen ? 'open' : ''}>
          <a href="#catalog">Каталог</a><a href="#categories">Категории</a><a href="#about">О проекте</a>
        </nav>
        <div className="header-actions">
          <span className={`connection ${backend}`}><i/>{backend === 'online' ? 'API подключён' : backend === 'checking' ? 'Проверяем API' : 'API недоступен'}</span>
          <button className="cart-button" onClick={onCart} aria-label="Открыть корзину">
            <ShoppingCart size={20}/>{cartCount > 0 && <b>{cartCount}</b>}
          </button>
          <button className="account-button" onClick={session ? onWorkspace : onAuth}>
            <CircleUserRound size={20}/><span>{session ? session.profile.username : 'Войти'}</span>
          </button>
          <button className="menu-button" onClick={() => setMenuOpen(!menuOpen)} aria-label="Меню"><Menu size={22}/></button>
        </div>
      </div>
    </header>
  )
}

function ProductCard({ product, index, favorite, onFavorite, onOpen }) {
  const accent = product.accent || accentCycle[index % accentCycle.length]
  return (
    <article className="product-card" style={{ '--card-index': index }}>
      <div className={`product-visual ${accent}`}>
        <span className="product-tag">{product.tag || product.status}</span>
        {product.primaryImageUrl
          ? <img className="product-photo" src={product.primaryImageUrl} alt={product.name}/>
          : <div className="product-shape"><span>{product.name.slice(0, 1)}</span></div>}
        <button
          className={favorite ? 'favorite active' : 'favorite'}
          aria-label={favorite ? 'Удалить из избранного' : 'Добавить в избранное'}
          aria-pressed={favorite}
          onClick={onFavorite}
        >{favorite ? '♥' : '♡'}</button>
      </div>
      <div className="product-info">
        <span className="product-meta">{product.status === 'PUBLISHED' ? 'В наличии' : product.status}</span>
        <h3>{product.name}</h3>
        <p>{product.description || 'Описание появится совсем скоро.'}</p>
        <div className="product-bottom">
          <strong>{formatPrice(product.price)}</strong>
          <button className="product-more" onClick={onOpen} aria-label={`Подробнее: ${product.name}`}><ArrowRight size={18}/></button>
        </div>
      </div>
    </article>
  )
}

function CatalogSkeleton() {
  return Array.from({ length: 6 }, (_, index) => (
    <div className="product-card skeleton-card" key={index} aria-hidden="true" style={{ '--card-index': index }}>
      <div className="skeleton-block skeleton-visual" />
      <div className="skeleton-copy">
        <div className="skeleton-block skeleton-line short" />
        <div className="skeleton-block skeleton-line title" />
        <div className="skeleton-block skeleton-line" />
        <div className="skeleton-block skeleton-line medium" />
      </div>
    </div>
  ))
}

function ProductDetailModal({ product, favorite, onFavorite, onAddCart, cartBusy, onClose }) {
  const images = product.images || []
  const [activeImage, setActiveImage] = useState(
    images.find(image => image.primary)?.url || images[0]?.url || product.primaryImageUrl || '',
  )
  const [quantity, setQuantity] = useState(1)

  return (
    <div className="modal-backdrop" onMouseDown={event => event.target === event.currentTarget && onClose()}>
      <div className="product-modal">
        <button className="icon-close" onClick={onClose} aria-label="Закрыть"><X size={20}/></button>
        <div className="product-modal-gallery">
          {activeImage
            ? <img src={activeImage} alt={product.name}/>
            : <div className="product-modal-placeholder">{product.name.slice(0, 1)}</div>}
          {images.length > 1 && <div className="product-thumbnails">
            {images.map(image => <button key={image.id} className={activeImage === image.url ? 'active' : ''} onClick={() => setActiveImage(image.url)}>
              <img src={image.url} alt={image.originalFilename || product.name}/>
            </button>)}
          </div>}
        </div>
        <div className="product-modal-copy">
          <span className="section-kicker">{product.status === 'PUBLISHED' ? 'В наличии' : product.status}</span>
          <h2>{product.name}</h2>
          <p>{product.description || 'Описание появится совсем скоро.'}</p>
          <strong>{formatPrice(product.price)}</strong>
          <div className="buy-row">
            <div className="quantity-control">
              <button onClick={() => setQuantity(value => Math.max(1, value - 1))} aria-label="Уменьшить"><Minus size={16}/></button>
              <span>{quantity}</span>
              <button onClick={() => setQuantity(value => value + 1)} aria-label="Увеличить"><Plus size={16}/></button>
            </div>
            <button className="primary-action" disabled={cartBusy} onClick={() => onAddCart(quantity)}>
              <ShoppingCart size={17}/>{cartBusy ? 'Добавляем…' : 'В корзину'}
            </button>
          </div>
          <div className="product-modal-actions">
            <button className="secondary-action" onClick={onFavorite}>{favorite ? 'Убрать из избранного' : 'В избранное'}</button>
            <button className="secondary-action" onClick={onClose}>Продолжить покупки</button>
          </div>
          <small>Избранное сохраняется в этом браузере. Серверный модуль избранного в backend пока является заготовкой.</small>
        </div>
      </div>
    </div>
  )
}

function CartDrawer({ cart, busy, onClose, onCart, onBusy, onToast, onOrderCreated }) {
  async function update(item, quantity) {
    onBusy(true)
    try {
      const nextCart = quantity <= 0
        ? await endpoints.removeCartItem(item.itemId)
        : await endpoints.updateCartItem(item.itemId, quantity)
      onCart(nextCart)
    } catch (error) {
      onToast(error.message || 'Не удалось обновить корзину')
    } finally {
      onBusy(false)
    }
  }

  async function checkout() {
    if (!cart.items?.length) return
    onBusy(true)
    try {
      const order = await endpoints.createOrder()
      onToast(`Заказ ${String(order.orderId).slice(0, 8)} оформлен`)
      onOrderCreated(order)
    } catch (error) {
      onToast(error.message || 'Не удалось оформить заказ')
    } finally {
      onBusy(false)
    }
  }

  return <div className="drawer-backdrop" onMouseDown={event => event.target === event.currentTarget && onClose()}>
    <aside className="cart-drawer">
      <div className="cart-head">
        <div><span className="section-kicker">Ваш заказ</span><h2>Корзина</h2></div>
        <button className="icon-close" onClick={onClose}><X size={20}/></button>
      </div>
      <div className="cart-items">
        {cart.items?.map(item => <article className="cart-item" key={item.itemId}>
          <div className="cart-item-mark">{item.productName?.slice(0, 1) || 'M'}</div>
          <div className="grow"><b>{item.productName}</b><small>{formatPrice(item.unitPrice)} за единицу</small></div>
          <div className="quantity-control compact">
            <button disabled={busy} onClick={() => update(item, item.quantity - 1)}><Minus size={14}/></button>
            <span>{item.quantity}</span>
            <button disabled={busy} onClick={() => update(item, item.quantity + 1)}><Plus size={14}/></button>
          </div>
          <strong>{formatPrice(item.subtotal)}</strong>
          <button className="cart-remove" disabled={busy} onClick={() => update(item, 0)} aria-label="Удалить"><Trash2 size={16}/></button>
        </article>)}
        {!cart.items?.length && <div className="cart-empty"><ShoppingCart size={34}/><h3>Корзина пуста</h3><p>Добавьте товары из каталога, чтобы оформить заказ.</p></div>}
      </div>
      <div className="cart-summary">
        <div><span>Товаров</span><b>{cart.totalItems || 0}</b></div>
        <div className="cart-total"><span>Итого</span><strong>{formatPrice(cart.totalPrice || 0)}</strong></div>
        <button className="primary-submit" disabled={busy || !cart.items?.length} onClick={checkout}>
          {busy ? 'Обрабатываем…' : 'Оформить заказ'}
        </button>
        <small>Оплата пока не подключена: заказ будет создан со статусом PENDING.</small>
      </div>
    </aside>
  </div>
}

function AuthModal({ onClose, onSession, onToast }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ username: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(event) {
    event.preventDefault()
    setLoading(true); setError('')
    try {
      if (mode === 'register') {
        await endpoints.register(form)
        onToast('Аккаунт создан — теперь войдите')
        setMode('login')
      } else {
        const token = await endpoints.login(form)
        authStore.set(token.accessToken)
        const profile = await endpoints.profile()
        onSession({ profile, role: 'USER' })
        onToast(`Добро пожаловать, ${profile.username}`)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Backend недоступен. Запустите Spring Boot и повторите попытку.')
    } finally { setLoading(false) }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="auth-modal">
        <button className="icon-close" onClick={onClose}><X size={20}/></button>
        <div className="auth-brand"><span className="brand-mark">M</span><div><b>Добро пожаловать</b><small>Ваш marketplace-аккаунт</small></div></div>
        <div className="segmented"><button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Вход</button><button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Регистрация</button></div>
        <form onSubmit={submit}>
          <label>Логин<input required maxLength={50} value={form.username} onChange={e => setForm({...form, username: e.target.value})} placeholder="например, ziyo"/></label>
          <label>Пароль<input required minLength={8} type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} placeholder="минимум 8 символов"/></label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary-submit" disabled={loading}>{loading ? 'Подождите…' : mode === 'login' ? 'Войти в аккаунт' : 'Создать аккаунт'}</button>
        </form>
        <p className="auth-note">Аккаунт и действия сохраняются в PostgreSQL через Marketplace API.</p>
      </div>
    </div>
  )
}

function Workspace({ session, categories, onClose, onLogout, onToast, onCatalogRefresh }) {
  const [tab, setTab] = useState('overview')
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState({ profile: session.profile, seller: null, store: null, products: [], pending: [], orders: [] })
  const [role, setRole] = useState(session.role || 'USER')

  useEffect(() => { loadWorkspace() }, [])

  async function loadWorkspace() {
    setLoading(true)
    const [seller, store, products, pending, orders] = await Promise.allSettled([
      endpoints.sellerProfile(), endpoints.myStore(), endpoints.myProducts(), endpoints.pendingSellers(), endpoints.orders(),
    ])
    const isAdmin = pending.status === 'fulfilled'
    const sellerProfile = seller.status === 'fulfilled' ? seller.value : null
    const isSeller = sellerProfile?.status === 'APPROVED'
    setRole(isAdmin ? 'ADMIN' : isSeller ? 'SELLER' : 'CUSTOMER')
    setData(current => ({
      ...current,
      seller: sellerProfile,
      store: store.status === 'fulfilled' ? store.value : null,
      products: products.status === 'fulfilled' ? normalizePage(products.value) : [],
      pending: isAdmin ? pending.value : [],
      orders: orders.status === 'fulfilled' ? normalizePage(orders.value) : [],
    }))
    setLoading(false)
  }

  const tabs = [
    ['overview', LayoutDashboard, 'Обзор'],
    ['orders', ShoppingBag, 'Мои заказы'],
    ...(role === 'SELLER' ? [['store', Store, 'Магазин'], ['products', Boxes, 'Товары']] : []),
    ...(role === 'ADMIN' ? [['admin', ShieldCheck, 'Модерация'], ['categories', ShoppingBag, 'Категории']] : []),
    ...(role === 'CUSTOMER' ? [['seller', UserRoundCheck, data.seller ? 'Заявка продавца' : 'Стать продавцом']] : []),
  ]

  return (
    <div className="workspace-backdrop">
      <aside className="workspace-sidebar">
        <div className="workspace-logo"><span className="brand-mark">M</span><b>Кабинет</b></div>
        <div className="workspace-user"><div>{session.profile.username.slice(0,2).toUpperCase()}</div><span><b>{session.profile.username}</b><small>{role}</small></span></div>
        <nav>{tabs.map(([key, Icon, label]) => <button key={key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}><Icon size={18}/>{label}</button>)}</nav>
        <button className="logout-button" onClick={onLogout}><LogOut size={18}/>Выйти</button>
      </aside>
      <section className="workspace-content">
        <div className="workspace-top"><div><span>LIVE API</span><h2>{tabTitle(tab)}</h2></div><button className="icon-close" onClick={onClose}><X size={21}/></button></div>
        {loading ? <div className="workspace-loading"><i/><span>Загружаем данные кабинета…</span></div> : (
          <WorkspaceTab
            tab={tab} role={role} data={data} categories={categories}
            reload={loadWorkspace} onToast={onToast} onCatalogRefresh={onCatalogRefresh}
          />
        )}
      </section>
    </div>
  )
}

function tabTitle(tab) {
  return ({ overview: 'Обзор аккаунта', orders: 'Мои заказы', store: 'Мой магазин', products: 'Мои товары', admin: 'Заявки продавцов', categories: 'Категории', seller: 'Стать продавцом' })[tab]
}

function WorkspaceTab({ tab, role, data, categories, reload, onToast, onCatalogRefresh }) {
  if (tab === 'overview') return <Overview role={role} data={data}/>
  if (tab === 'orders') return <OrdersPanel orders={data.orders} reload={reload} onToast={onToast}/>
  if (tab === 'seller') return <SellerApply seller={data.seller} reload={reload} onToast={onToast}/>
  if (tab === 'store') return <StorePanel store={data.store} reload={reload} onToast={onToast}/>
  if (tab === 'products') return <ProductsPanel products={data.products} categories={categories} reload={reload} onToast={onToast} onCatalogRefresh={onCatalogRefresh}/>
  if (tab === 'admin') return <AdminPanel pending={data.pending} reload={reload} onToast={onToast}/>
  if (tab === 'categories') return <CategoryPanel categories={categories} onToast={onToast} onCatalogRefresh={onCatalogRefresh}/>
}

function Overview({ role, data }) {
  return <div className="workspace-grid">
    <div className="metric-card"><span><CircleUserRound/></span><small>Профиль</small><strong>{data.profile.status}</strong><p>{data.profile.username}</p></div>
    <div className="metric-card"><span><UserRoundCheck/></span><small>Seller</small><strong>{data.seller?.status || 'Не подключён'}</strong><p>{data.seller ? 'Профиль продавца создан' : 'Можно подать заявку'}</p></div>
    <div className="metric-card"><span><Store/></span><small>Магазин</small><strong>{data.store?.name || 'Нет магазина'}</strong><p>{data.store?.active ? 'Активен и виден клиентам' : 'Ожидает настройки'}</p></div>
    <div className="metric-card"><span><Boxes/></span><small>Товары</small><strong>{data.products.length}</strong><p>в текущей витрине</p></div>
    <div className="workspace-welcome"><div><span className="section-kicker">{role}</span><h3>Всё под контролем.</h3><p>Этот кабинет работает с теми же REST endpoints, которые вы создали в Spring Boot.</p></div><div className="welcome-orb"><ShieldCheck/></div></div>
  </div>
}

function OrdersPanel({ orders, reload, onToast }) {
  const [busyId, setBusyId] = useState(null)

  async function cancel(orderId) {
    if (!window.confirm('Отменить этот заказ и освободить зарезервированный товар?')) return
    setBusyId(orderId)
    try {
      await endpoints.cancelOrder(orderId)
      onToast('Заказ отменён')
      reload()
    } catch (error) {
      onToast(error.message || 'Не удалось отменить заказ')
    } finally {
      setBusyId(null)
    }
  }

  if (!orders.length) return <div className="single-panel"><ShoppingBag size={34}/><h3>Заказов пока нет</h3><p>Добавьте товары в корзину и оформите первый заказ.</p></div>

  return <div className="orders-list">
    {orders.map(order => <article className="order-card" key={order.orderId}>
      <div className="order-card-head">
        <div><small>Заказ от {new Date(order.createdAt).toLocaleString('ru-RU')}</small><b>№ {String(order.orderId).slice(0, 8)}</b></div>
        <div><span className={`status-badge ${String(order.status).toLowerCase()}`}>{order.status}</span><strong>{formatPrice(order.totalAmount)}</strong></div>
      </div>
      <div className="seller-order-list">
        {(order.sellerOrders || []).map(part => <section className="seller-order-block" key={part.sellerOrderId}>
          <div><b>Магазин · {String(part.storeId).slice(0, 8)}</b><span className={`status-badge ${String(part.status).toLowerCase()}`}>{part.status}</span></div>
          {(part.items || []).map(item => <div className="order-line" key={item.itemId}>
            <span>{item.productName}</span><small>{item.quantity} × {formatPrice(item.unitPrice)}</small><b>{formatPrice(item.subtotal)}</b>
          </div>)}
        </section>)}
        {!order.sellerOrders?.length && <p className="transition-note">Состав заказа ещё формируется текущей версией backend.</p>}
      </div>
      {order.status === 'PENDING' && <button className="secondary-action danger-text" disabled={busyId === order.orderId} onClick={() => cancel(order.orderId)}>{busyId === order.orderId ? 'Отменяем…' : 'Отменить заказ'}</button>}
    </article>)}
  </div>
}

function SellerApply({ seller, reload, onToast }) {
  async function apply() {
    try { await endpoints.applySeller(); onToast('Заявка продавца отправлена'); reload() } catch (e) { onToast(e.message) }
  }
  if (seller) return <div className="single-panel"><UserRoundCheck size={34}/><h3>Статус заявки: {seller.status}</h3><p>{seller.rejectionReason || (seller.status === 'PENDING' ? 'Администратор ещё рассматривает заявку.' : 'Профиль продавца создан.')}</p>{seller.status === 'REJECTED' && <button className="primary-action" onClick={apply}>Подать повторно</button>}</div>
  return <div className="single-panel"><UserRoundCheck size={34}/><h3>Откройте магазин на Makon</h3><p>После подачи заявки администратор проверит профиль. Одобренный продавец получит роль SELLER.</p><button className="primary-action" onClick={apply}>Подать заявку</button></div>
}

function StorePanel({ store, reload, onToast }) {
  const [form, setForm] = useState(store || { name: '', slug: '', description: '' })
  async function save(e) {
    e.preventDefault()
    try { store ? await endpoints.updateStore(form) : await endpoints.createStore(form); onToast('Магазин сохранён'); reload() } catch (err) { onToast(err.message) }
  }
  async function toggle() {
    try { await endpoints.toggleStore(store.active ? 'deactivate' : 'activate'); reload() } catch (err) { onToast(err.message) }
  }
  return <div className="form-panel"><div className="panel-title"><div><span className={`status-dot ${store?.active ? 'active' : ''}`}/><h3>{store ? store.name : 'Новый магазин'}</h3></div>{store && <button className="secondary-action" onClick={toggle}>{store.active ? 'Деактивировать' : 'Активировать'}</button>}</div><form onSubmit={save}><label>Название<input value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required maxLength={150}/></label><label>Slug<input value={form.slug} onChange={e=>setForm({...form,slug:e.target.value})} required maxLength={180} pattern="^[a-z0-9]+(?:-[a-z0-9]+)*$"/></label><label className="full">Описание<textarea value={form.description || ''} onChange={e=>setForm({...form,description:e.target.value})} maxLength={1000}/></label><button className="primary-action">Сохранить магазин</button></form></div>
}

function ProductsPanel({ products, categories, reload, onToast, onCatalogRefresh }) {
  const [creating, setCreating] = useState(false)
  const [imageProduct, setImageProduct] = useState(null)
  const [form, setForm] = useState({ categoryId: categories[0]?.id || '', name: '', slug: '', description: '', price: '' })

  useEffect(() => {
    if (!form.categoryId && categories[0]?.id) {
      setForm(current => ({ ...current, categoryId: categories[0].id }))
    }
  }, [categories, form.categoryId])

  async function create(e) {
    e.preventDefault()
    try {
      await endpoints.createProduct({ ...form, price: Number(form.price) })
      onToast('Черновик товара создан')
      setCreating(false)
      setForm({ categoryId: categories[0]?.id || '', name: '', slug: '', description: '', price: '' })
      reload()
    } catch (err) { onToast(err.message) }
  }

  async function act(id, action) {
    try { await endpoints.productAction(id, action); onToast('Статус товара обновлён'); reload(); onCatalogRefresh() } catch (err) { onToast(err.message) }
  }

  return <div>
    <div className="panel-actions">
      <p>Черновики, цены, публикации и фотографии товара.</p>
      <button className="primary-action" onClick={() => setCreating(!creating)}>{creating ? 'Закрыть' : '+ Новый товар'}</button>
    </div>

    {creating && <form className="inline-form" onSubmit={create}>
      <label>Категория<select required value={form.categoryId} onChange={e => setForm({...form, categoryId: e.target.value})}>{categories.map(c => <option value={c.id} key={c.id}>{c.name}</option>)}</select></label>
      <label>Название<input required maxLength={200} value={form.name} onChange={e => setForm({...form, name: e.target.value})}/></label>
      <label>Slug<input required maxLength={220} pattern="^[a-z0-9]+(?:-[a-z0-9]+)*$" value={form.slug} onChange={e => setForm({...form, slug: e.target.value})}/></label>
      <label>Цена, сум<input required type="number" min="0.01" step="0.01" value={form.price} onChange={e => setForm({...form, price: e.target.value})}/></label>
      <label className="full">Описание<textarea value={form.description} onChange={e => setForm({...form, description: e.target.value})}/></label>
      <button className="primary-action">Создать черновик</button>
    </form>}

    <div className="data-list">
      {products.map(product => <div className="product-workspace-item" key={product.id}>
        <div className="data-row">
          <div className={`mini-product ${product.accent || 'violet'}`}>{product.name[0]}</div>
          <div className="grow"><b>{product.name}</b><small>{product.slug} · {formatPrice(product.price)}</small></div>
          <span className={`status-badge ${product.status.toLowerCase()}`}>{product.status}</span>
          <div className="row-actions">
            <button className={imageProduct?.id === product.id ? 'active' : ''} onClick={() => setImageProduct(imageProduct?.id === product.id ? null : product)}><Boxes size={14}/> Управление</button>
            {product.status === 'DRAFT' && <button onClick={() => act(product.id, 'publish')}>Опубликовать</button>}
            {product.status === 'PUBLISHED' && <button onClick={() => act(product.id, 'archive')}>В архив</button>}
            {product.status === 'ARCHIVED' && <button onClick={() => act(product.id, 'restore')}>В черновик</button>}
          </div>
        </div>
        {imageProduct?.id === product.id && <ProductToolsPanel product={product} onToast={onToast}/>} 
      </div>)}
      {!products.length && <div className="list-empty">Товаров пока нет</div>}
    </div>
  </div>
}

function ProductToolsPanel({ product, onToast }) {
  const [inventory, setInventory] = useState(null)
  const [amount, setAmount] = useState(1)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  useEffect(() => { loadInventory() }, [product.id])

  async function loadInventory() {
    setLoading(true)
    try {
      setInventory(await endpoints.inventory(product.id))
    } catch (error) {
      onToast(error.message || 'Не удалось загрузить остаток')
    } finally {
      setLoading(false)
    }
  }

  async function changeStock(action) {
    const numericAmount = Number(amount)
    if (!Number.isInteger(numericAmount) || numericAmount <= 0) return onToast('Укажите целое количество больше нуля')
    setBusy(true)
    try {
      const next = action === 'add'
        ? await endpoints.addStock(product.id, numericAmount)
        : await endpoints.removeStock(product.id, numericAmount)
      setInventory(next)
      onToast(action === 'add' ? 'Остаток пополнен' : 'Остаток уменьшен')
    } catch (error) {
      onToast(error.message || 'Не удалось изменить остаток')
    } finally {
      setBusy(false)
    }
  }

  return <div className="product-tools">
    <section className="inventory-card">
      <div><span className="section-kicker">Склад</span><h4>Остатки товара</h4></div>
      {loading ? <p>Загружаем остаток…</p> : inventory ? <>
        <div className="inventory-metrics">
          <div><small>Всего</small><b>{inventory.quantity}</b></div>
          <div><small>В резерве</small><b>{inventory.reservedQuantity}</b></div>
          <div><small>Доступно</small><b>{inventory.availableQuantity}</b></div>
        </div>
        <div className="stock-actions">
          <input type="number" min="1" step="1" value={amount} onChange={event => setAmount(event.target.value)}/>
          <button disabled={busy} className="approve" onClick={() => changeStock('add')}>+ Добавить</button>
          <button disabled={busy} onClick={() => changeStock('remove')}>− Убрать</button>
        </div>
      </> : <p>Inventory для товара не найден.</p>}
    </section>
    <ProductImagesPanel product={product} onToast={onToast}/>
  </div>
}

function ProductImagesPanel({ product, onToast }) {
  const [images, setImages] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)

  useEffect(() => { loadImages() }, [product.id])

  async function loadImages() {
    setLoading(true)
    try {
      setImages(await endpoints.myProductImages(product.id))
    } catch (error) {
      onToast(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function upload(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setUploading(true)
    try {
      await endpoints.uploadProductImage(product.id, file)
      await loadImages()
      onToast('Изображение загружено')
    } catch (error) {
      onToast(error.message)
    } finally {
      setUploading(false)
    }
  }

  async function makePrimary(imageId) {
    try {
      await endpoints.setPrimaryProductImage(product.id, imageId)
      await loadImages()
      onToast('Главное изображение обновлено')
    } catch (error) { onToast(error.message) }
  }

  async function remove(imageId) {
    if (!window.confirm('Удалить это изображение?')) return
    try {
      await endpoints.deleteProductImage(product.id, imageId)
      await loadImages()
      onToast('Изображение удалено')
    } catch (error) { onToast(error.message) }
  }

  async function move(index, direction) {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= images.length) return
    const reordered = [...images]
    const [moved] = reordered.splice(index, 1)
    reordered.splice(nextIndex, 0, moved)
    try {
      setImages(await endpoints.reorderProductImages(product.id, reordered.map(image => image.id)))
    } catch (error) { onToast(error.message) }
  }

  return <div className="image-manager">
    <div className="image-manager-head">
      <div><b>Фотографии · {product.name}</b><small>JPEG, PNG или WebP · до 5 МБ · максимум 10</small></div>
      <div className="image-manager-actions">
        <button className="secondary-action" onClick={loadImages}>Обновить ссылки</button>
        <label className={`image-upload ${uploading ? 'disabled' : ''}`}><UploadCloud size={16}/>{uploading ? 'Загрузка…' : 'Добавить фото'}<input type="file" accept="image/jpeg,image/png,image/webp" onChange={upload} disabled={uploading}/></label>
      </div>
    </div>

    {loading ? <div className="image-loading">Загружаем фотографии…</div> : images.length ? (
      <div className="image-grid">
        {images.map((image, index) => <article className={`image-card ${image.primary ? 'primary' : ''}`} key={image.id}>
          <div className="image-preview">
            <img src={image.url} alt={image.originalFilename || product.name}/>
            {image.primary && <span><Star size={12} fill="currentColor"/> Главное</span>}
          </div>
          <div className="image-card-meta"><b>{index + 1}. {image.originalFilename}</b><small>{Math.max(1, Math.round(image.sizeBytes / 1024))} КБ</small></div>
          <div className="image-card-actions">
            <button disabled={index === 0} onClick={() => move(index, -1)} aria-label="Переместить выше"><ChevronUp size={15}/></button>
            <button disabled={index === images.length - 1} onClick={() => move(index, 1)} aria-label="Переместить ниже"><ChevronDown size={15}/></button>
            {!image.primary && <button onClick={() => makePrimary(image.id)} title="Сделать главным"><Star size={15}/></button>}
            <button className="danger" onClick={() => remove(image.id)} title="Удалить"><Trash2 size={15}/></button>
          </div>
        </article>)}
      </div>
    ) : <div className="image-empty"><ImagePlus size={26}/><span>У товара пока нет фотографий</span><small>Первая загруженная фотография автоматически станет главной.</small></div>}
  </div>
}

function AdminPanel({ pending, reload, onToast }) {
  async function moderate(id, action) {
    try { await endpoints.moderateSeller(id, action); onToast('Заявка обработана'); reload() } catch (err) { onToast(err.message) }
  }
  return <div className="data-list admin-list">{pending.map(item=><div className="data-row" key={item.id}><div className="avatar-small">{item.userId.slice(0,2).toUpperCase()}</div><div className="grow"><b>Заявка продавца</b><small>User ID: {item.userId}</small></div><span className="status-badge draft">PENDING</span><div className="row-actions"><button className="approve" onClick={()=>moderate(item.id,'approve')}>Одобрить</button><button onClick={()=>moderate(item.id,'reject')}>Отклонить</button></div></div>)}{!pending.length && <div className="list-empty"><BadgeCheck size={28}/>Новых заявок нет</div>}</div>
}

function CategoryPanel({ categories, onToast, onCatalogRefresh }) {
  const [form, setForm] = useState({ name: '', slug: '', description: '', parentId: null })
  async function submit(e) {
    e.preventDefault()
    try { await endpoints.createCategory(form); onToast('Категория создана'); onCatalogRefresh(); setForm({name:'',slug:'',description:'',parentId:null}) } catch (err) { onToast(err.message) }
  }
  return <div className="form-panel"><div className="panel-title"><h3>Новая категория</h3><span>{categories.length} активных</span></div><form onSubmit={submit}><label>Название<input required maxLength={100} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Slug<input required maxLength={120} value={form.slug} onChange={e=>setForm({...form,slug:e.target.value})}/></label><label className="full">Описание<textarea maxLength={1000} value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/></label><button className="primary-action">Создать категорию</button></form></div>
}

function Footer() {
  return <footer id="about"><div className="section-wrap footer-inner"><a className="brand" href="#"><span className="brand-mark">M</span><span>makon<small>market</small></span></a><p>Marketplace · Java 21 + Spring Boot + React</p><span>2026</span></div></footer>
}

export default App
