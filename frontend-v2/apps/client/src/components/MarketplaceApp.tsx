'use client';

import Image from 'next/image';
import { useEffect, useRef, useState } from 'react';
import { FiArrowDown, FiArrowRight, FiCheckCircle, FiGlobe, FiPackage, FiSearch, FiShield, FiShoppingBag, FiStar, FiTruck } from 'react-icons/fi';
import { api, authStore, emptyCart, type Cart, type Category, type Product, type Store, type UserProfile } from '@makon/api-client';
import { MarketplaceHeader } from './layout/MarketplaceHeader';
import { MarketplaceFooter } from './layout/MarketplaceFooter';
import { CatalogSection, type CatalogFilters } from '../features/catalog/CatalogSection';
import { ProductDialog } from '../features/catalog/ProductDialog';
import { AuthDialog } from '../features/auth/AuthDialog';
import { CartDrawer } from '../features/cart/CartDrawer';
import { AccountDrawer } from '../features/account/AccountDrawer';
import { fallbackProductImage } from '../features/catalog/product-images';

const initialFilters: CatalogFilters = { search: '', categoryId: 'all', minPrice: '', maxPrice: '', sort: 'createdAt,desc', storeSlug: 'all', availableOnly: 'false' };

type InitialPanel = 'home' | 'catalog' | 'cart' | 'login' | 'register';

export function MarketplaceApp({ initialPanel = 'home' }: { initialPanel?: InitialPanel }) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [filters, setFilters] = useState<CatalogFilters>(initialFilters);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [pageIndex, setPageIndex] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const catalogRequest = useRef(0);
  const [apiOnline, setApiOnline] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [cart, setCart] = useState<Cart>(emptyCart);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<Product | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [cartOpen, setCartOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [toast, setToast] = useState('');
  const [ordersKey, setOrdersKey] = useState(0);

  useEffect(() => {
    void loadCatalog(initialFilters);
    if (authStore.get()) void restoreSession();
    if (initialPanel === 'login' || initialPanel === 'register') setAuthOpen(true);
    if (initialPanel === 'cart') {
      if (authStore.get()) setCartOpen(true);
      else setAuthOpen(true);
    }
    if (initialPanel === 'catalog') requestAnimationFrame(() => document.getElementById('catalog')?.scrollIntoView());
  }, []);
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 3500); return () => window.clearTimeout(timer); }, [toast]);

  async function restoreSession() {
    try { const current = await api.profile(); setProfile(current); setCart(await api.cart()); await loadFavorites(); }
    catch { authStore.clear(); setProfile(null); setFavorites(new Set()); }
  }

  async function loadFavorites() {
    const page = await api.favorites();
    setFavorites(new Set(page.content.map((item) => item.product.id)));
  }

  async function loadCatalog(nextFilters = filters, nextPage = 0) {
    const requestId = ++catalogRequest.current;
    if (nextPage === 0) setLoading(true); else setLoadingMore(true);
    try {
      const [categoryList, storePage, page] = await Promise.all([api.categories(), api.stores(), api.products({ ...nextFilters, availableOnly: nextFilters.availableOnly === 'true', page: nextPage, size: 12 })]);
      const enriched = await Promise.all(page.content.map(async (product) => {
        try {
          const images = await api.publicProductImages(product.storeSlug, product.slug);
          const primary = images.find((image) => image.primary) || images[0];
          return { ...product, images, primaryImageUrl: primary?.url || fallbackProductImage(product) };
        } catch { return { ...product, images: [], primaryImageUrl: fallbackProductImage(product) }; }
      }));
      if (requestId !== catalogRequest.current) return;
      setCategories(categoryList);
      setStores(storePage.content);
      setProducts((current) => nextPage === 0 ? enriched : [...current, ...enriched]);
      setTotalElements(page.totalElements); setPageIndex(page.number); setHasMore(!page.last); setApiOnline(true);
    } catch (error) { if (requestId === catalogRequest.current) { setApiOnline(false); setToast(error instanceof Error ? error.message : 'Не удалось загрузить каталог'); } }
    finally { if (requestId === catalogRequest.current) { setLoading(false); setLoadingMore(false); } }
  }

  async function toggleFavorite(product: Product) {
    if (!profile) { setAuthOpen(true); setToast('Войдите, чтобы сохранить товар'); return; }
    try {
      if (favorites.has(product.id)) await api.removeFavorite(product.id);
      else await api.addFavorite(product.id);
      setFavorites((current) => {
        const next = new Set(current);
        if (next.has(product.id)) next.delete(product.id); else next.add(product.id);
        return next;
      });
    } catch (error) { setToast(error instanceof Error ? error.message : 'Не удалось обновить избранное'); }
  }

  async function addToCart(product: Product, quantity = 1, variantId?: string) {
    if (!profile) { setAuthOpen(true); setToast('Войдите, чтобы добавить товар'); return; }
    try {
      if (!variantId && !selected) {
        const variants = await api.publicVariants(product.id);
        if (variants.length) { setSelected(product); return; }
      }
      setCart(await api.addCartItem(product.id, quantity, variantId));
      setToast(`${product.name} добавлен в корзину`);
    }
    catch (error) { setToast(error instanceof Error ? error.message : 'Не удалось добавить товар'); }
  }

  function authenticated(current: UserProfile) {
    setProfile(current); setAuthOpen(false); void api.cart().then(setCart).catch(() => setCart(emptyCart));
    void loadFavorites().catch(() => setFavorites(new Set()));
    if (initialPanel === 'cart') setCartOpen(true); else setAccountOpen(true);
  }
  async function logout() { try { await api.logout(); } catch { /* Local session still ends. */ } authStore.clear(); setProfile(null); setCart(emptyCart); setFavorites(new Set()); setAccountOpen(false); setToast('Вы вышли из аккаунта'); }
  function resetFilters() { setFilters(initialFilters); void loadCatalog(initialFilters); }

  return <div id="top" className="market-app">
    <MarketplaceHeader apiOnline={apiOnline} loading={loading} profile={profile} cartCount={cart.totalItems} onAuth={() => setAuthOpen(true)} onAccount={() => setAccountOpen(true)} onCart={() => profile ? setCartOpen(true) : setAuthOpen(true)} />
    <main>
      <section className="market-hero market-container">
        <div className="market-hero__copy"><p className="eyebrow"><FiStar />Новая витрина Узбекистана</p><h1>Выбирайте вещи,<br/><em>которые остаются.</em></h1><p>Технологии, дом, уход и спорт от проверенных продавцов — в одном выразительном пространстве.</p><div className="hero-search"><FiSearch /><input value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} onKeyDown={(event) => event.key === 'Enter' && loadCatalog()} placeholder="Что вы ищете сегодня?"/><button onClick={() => loadCatalog()}>Найти <FiArrowRight /></button></div><div className="hero-metrics"><div><b>{totalElements}</b><span>товаров по вашему запросу</span></div><div><b>{categories.length}</b><span>категорий</span></div><div><b>24/7</b><span>доступ к витрине</span></div></div></div>
        <div className="market-hero__visual"><div className="hero-orbit"/><Image src="/products/electronics.png" alt="Популярная электроника Makon" fill priority sizes="(max-width: 900px) 100vw, 48vw"/><div className="hero-floating hero-floating--top"><FiCheckCircle /><span><b>Проверено</b> продавец одобрен</span></div><div className="hero-floating hero-floating--bottom"><FiShoppingBag /><span><b>Живой каталог</b> реальные предложения</span></div><a href="#catalog" className="hero-scroll"><FiArrowDown />Смотреть товары</a></div>
      </section>

      <section className="category-showcase market-container" id="categories"><header className="section-title"><div><span>Направления</span><h2>Найдите своё</h2></div><p>Выберите раздел и откройте подходящие предложения.</p></header><div className="category-showcase__grid">{categories.filter((category) => !category.parentId && category.isActive).map((category, index) => <button key={category.id} onClick={() => { const next = { ...filters, categoryId: category.id }; setFilters(next); void loadCatalog(next); document.getElementById('catalog')?.scrollIntoView(); }}><span>{['⌁','⌂','✦','◉','◇'][index % 5]}</span><div><b>{category.name}</b><small>{category.description}</small></div><FiArrowRight /></button>)}</div></section>

      <CatalogSection categories={categories} stores={stores} products={products} loading={loading} loadingMore={loadingMore} totalElements={totalElements} hasMore={hasMore} filters={filters} favorites={favorites} onFilters={setFilters} onApply={() => loadCatalog()} onCategory={(categoryId) => { const next = { ...filters, categoryId }; setFilters(next); void loadCatalog(next); }} onMore={() => loadCatalog(filters, pageIndex + 1)} onReset={resetFilters} onOpen={setSelected} onFavorite={toggleFavorite} onAdd={(product) => addToCart(product)} />

      <section className="trust-section market-container" id="trust"><div className="trust-intro"><p className="eyebrow">Основа доверия</p><h2>Каждый этап<br/>под контролем.</h2><p>Покупки, заказы и история в одном личном кабинете.</p></div><div className="trust-grid"><article><FiShield /><span>01</span><h3>Личный кабинет</h3><p>Корзина и история заказов доступны после входа в аккаунт.</p></article><article><FiPackage /><span>02</span><h3>Учёт остатков</h3><p>Наличие товаров проверяется при оформлении заказа.</p></article><article><FiTruck /><span>03</span><h3>История заказа</h3><p>Цена и название товара сохраняются на момент покупки.</p></article><article><FiGlobe /><span>04</span><h3>Локальный бизнес</h3><p>Продавец проходит модерацию перед публикацией товаров.</p></article></div></section>

      <section className="seller-banner market-container"><div><p className="eyebrow">Для предпринимателей</p><h2>Откройте магазин<br/>в Makon.</h2><p>Управляйте товарами, остатками и изображениями через отдельный кабинет.</p></div><a href="http://localhost:4200" target="_blank" rel="noreferrer">Перейти в dashboard <FiArrowRight /></a></section>
    </main>
    <MarketplaceFooter />
    {selected && <ProductDialog product={selected} favorite={favorites.has(selected.id)} onFavorite={() => toggleFavorite(selected)} onAdd={(quantity, variantId) => addToCart(selected, quantity, variantId)} onToast={setToast} onClose={() => setSelected(null)} />}
    {authOpen && <AuthDialog initialMode={initialPanel === 'register' ? 'register' : 'login'} onClose={() => setAuthOpen(false)} onAuthenticated={authenticated} onToast={setToast} />}
    {cartOpen && <CartDrawer cart={cart} onCart={setCart} onClose={() => setCartOpen(false)} onToast={setToast} onOrder={() => { setCart(emptyCart); setCartOpen(false); setOrdersKey((value) => value + 1); setAccountOpen(true); }} />}
    {accountOpen && profile && <AccountDrawer profile={profile} refreshKey={ordersKey} onProfile={setProfile} onClose={() => setAccountOpen(false)} onLogout={logout} onToast={setToast} />}
    {toast && <div className="market-toast"><FiCheckCircle />{toast}</div>}
  </div>;
}
