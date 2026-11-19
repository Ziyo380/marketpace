import { useEffect, useState } from 'react';
import { FiGrid, FiHome, FiLogOut, FiPackage, FiShield, FiShoppingBag, FiUsers } from 'react-icons/fi';
import { api, ApiError, authStore, type Category, type Product, type Seller, type Store, type UserProfile } from '@makon/api-client';
import { useLiveRefresh } from '@makon/shared-ui';
import { DashboardLogin } from '../features/auth/DashboardLogin';
import { OverviewPanel } from '../features/overview/OverviewPanel';
import { StorePanel } from '../features/store/StorePanel';
import { ProductsPanel } from '../features/products/ProductsPanel';
import { ModerationPanel } from '../features/moderation/ModerationPanel';
import { SellerOrdersPanel } from '../features/orders/SellerOrdersPanel';
import { CategoriesPanel } from '../features/categories/CategoriesPanel';
import { AdminPanel } from '../features/moderation/AdminPanel';

export type Tab = 'overview' | 'store' | 'products' | 'orders' | 'categories' | 'moderation' | 'admin';
export type Workspace = { profile: UserProfile; seller: Seller | null; store: Store | null; products: Product[]; pending: Seller[]; categories: Category[]; canModerate: boolean };

export default function App() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [loading, setLoading] = useState(Boolean(authStore.get()));
  const [tab, setTab] = useState<Tab>('overview');
  const [toast, setToast] = useState('');

  useEffect(() => { if (authStore.get()) void loadWorkspace(); }, []);
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 3500); return () => window.clearTimeout(timer); }, [toast]);
  useEffect(() => {
    if (!workspace) return;
    if ((tab === 'categories' || tab === 'moderation' || tab === 'admin') && !workspace.canModerate) setTab('overview');
    if ((tab === 'products' || tab === 'orders') && workspace.seller?.status !== 'APPROVED') setTab('overview');
    if (tab === 'store' && workspace.canModerate && !workspace.seller) setTab('overview');
  }, [workspace, tab]);

  useLiveRefresh(async () => { if (!loading) await loadWorkspace(true); }, !!workspace);

  async function loadWorkspace(background = false) {
    if (!background) setLoading(true);
    try {
      const profile = await api.profile();
      const [categoriesResult, sellerResult, storeResult, productResult, pendingResult] = await Promise.allSettled([
        api.categories(), api.sellerProfile(), api.myStore(), api.myProducts(), api.pendingSellers(),
      ]);
      setWorkspace({
        profile,
        categories: categoriesResult.status === 'fulfilled' ? categoriesResult.value : [],
        seller: sellerResult.status === 'fulfilled' ? sellerResult.value : null,
        store: storeResult.status === 'fulfilled' ? storeResult.value : null,
        products: productResult.status === 'fulfilled' ? productResult.value.content : [],
        pending: pendingResult.status === 'fulfilled' ? pendingResult.value : [],
        canModerate: pendingResult.status === 'fulfilled',
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) { authStore.clear(); setWorkspace(null); }
      else if (!background) setToast(error instanceof Error ? error.message : 'Не удалось обновить кабинет');
    }
    finally { if (!background) setLoading(false); }
  }

  async function logout() { try { await api.logout(); } catch { /* Local session still ends. */ } authStore.clear(); setWorkspace(null); setTab('overview'); setToast('Сессия завершена'); }
  if (!workspace && !loading) return <DashboardLogin onReady={async () => { setTab('overview'); await loadWorkspace(); }} onToast={setToast} toast={toast} />;
  if (!workspace) return <div className="dashboard-loading"><i/><b>Подключаем кабинет</b><span>Загружаем данные вашего аккаунта</span></div>;

  const navigation: Array<{ id: Tab; label: string; icon: typeof FiGrid }> = [
    { id: 'overview', label: 'Обзор', icon: FiGrid },
    ...(!workspace.canModerate || workspace.seller ? [{ id: 'store' as Tab, label: 'Магазин', icon: FiHome }] : []),
    ...(workspace.seller?.status === 'APPROVED' ? [{ id: 'products' as Tab, label: 'Товары', icon: FiPackage }, { id: 'orders' as Tab, label: 'Заказы', icon: FiShoppingBag }] : []),
    ...(workspace.canModerate ? [{ id: 'categories' as Tab, label: 'Категории', icon: FiGrid }, { id: 'moderation' as Tab, label: 'Продавцы', icon: FiUsers }, { id: 'admin' as Tab, label: 'Управление', icon: FiShield }] : []),
  ];
  return <div className="dashboard-shell">
    <aside className="dashboard-sidebar"><a className="dashboard-brand" href="http://localhost:3000/ru"><span>M</span><div><b>MAKON</b><small>workspace</small></div></a><div className="dashboard-user"><div>{workspace.profile.username.slice(0, 2).toUpperCase()}</div><span><b>{workspace.profile.username}</b><small>{workspace.seller?.status || workspace.profile.status}</small></span></div><nav>{navigation.map(({ id, label, icon: Icon }) => <button key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}><Icon />{label}{id === 'moderation' && workspace.pending.length > 0 && <b>{workspace.pending.length}</b>}</button>)}</nav><a className="customer-link" href="http://localhost:3000/ru" target="_blank" rel="noreferrer"><FiShoppingBag />Открыть витрину</a><button className="dashboard-logout" onClick={logout}><FiLogOut />Выйти</button></aside>
    <main className="dashboard-main"><header className="dashboard-top"><div><span>MAKON CONTROL CENTER</span><h1>{navigation.find((item) => item.id === tab)?.label}</h1></div><div className="backend-badge"><i/>Сервис доступен</div></header>
      {tab === 'overview' && <OverviewPanel workspace={workspace} onNavigate={setTab} />}
      {tab === 'store' && <StorePanel seller={workspace.seller} store={workspace.store} onReload={loadWorkspace} onToast={setToast} />}
      {tab === 'products' && <ProductsPanel products={workspace.products} categories={workspace.categories} onReload={loadWorkspace} onToast={setToast} />}
      {tab === 'orders' && <SellerOrdersPanel seller={workspace.seller} onToast={setToast} />}
      {tab === 'categories' && <CategoriesPanel categories={workspace.categories} onReload={loadWorkspace} onToast={setToast} />}
      {tab === 'moderation' && <ModerationPanel sellers={workspace.pending} onReload={loadWorkspace} onToast={setToast} />}
      {tab === 'admin' && <AdminPanel onToast={setToast} />}
    </main>
    {toast && <div className="dashboard-toast"><FiShield />{toast}</div>}
  </div>;
}
