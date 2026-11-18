import { FiActivity, FiArrowRight, FiBox, FiGrid, FiHome, FiPackage, FiShield, FiShoppingBag, FiUsers } from 'react-icons/fi';
import type { Tab, Workspace } from '../../app/app';

type Props = { workspace: Workspace; onNavigate: (tab: Tab) => void };

export function OverviewPanel({ workspace, onNavigate }: Props) {
  const published = workspace.products.filter((product) => product.status === 'PUBLISHED').length;
  const metrics = workspace.canModerate ? [
    { label: 'Заявки', value: workspace.pending.length, detail: 'Ожидают решения', icon: FiUsers, tab: 'moderation' as Tab },
    { label: 'Категории', value: workspace.categories.length, detail: 'Разделы каталога', icon: FiGrid, tab: 'categories' as Tab },
  ] : [
    { label: 'Товаров', value: workspace.products.length, detail: `${published} опубликовано`, icon: FiPackage, tab: workspace.seller?.status === 'APPROVED' ? 'products' as Tab : 'store' as Tab },
    { label: 'Магазин', value: workspace.store?.active ? 'Активен' : 'Не создан', detail: workspace.store?.name || 'Создайте витрину', icon: FiHome, tab: 'store' as Tab },
    { label: 'Продавец', value: workspace.seller?.status || 'Нет профиля', detail: 'Статус модерации', icon: FiShield, tab: 'store' as Tab },
  ];
  return <div className="dashboard-panel"><section className="welcome-card"><div><p>Рабочее пространство</p><h2>Добро пожаловать,<br/>{workspace.profile.username}.</h2><span>Здесь отображаются актуальные данные вашего аккаунта.</span></div><FiActivity /></section><section className="metric-grid">{metrics.map(({ label, value, detail, icon: Icon, tab }) => <button key={label} onClick={() => onNavigate(tab)}><div><Icon /><FiArrowRight /></div><span>{label}</span><strong>{value}</strong><small>{detail}</small></button>)}</section><section className="quick-grid"><article><FiShoppingBag /><h3>Клиентская витрина</h3><p>Посмотрите каталог так, как его видят покупатели.</p><a href="http://localhost:3000/ru" target="_blank" rel="noreferrer">Открыть витрину <FiArrowRight /></a></article><article><FiBox /><h3>{workspace.canModerate ? 'Заявки продавцов' : 'Управление остатками'}</h3><p>{workspace.canModerate ? 'Проверяйте новые заявки перед открытием магазинов.' : 'Количество и резерв находятся рядом с каждым товаром.'}</p><button onClick={() => onNavigate(workspace.canModerate ? 'moderation' : workspace.seller?.status === 'APPROVED' ? 'products' : 'store')}>Перейти в раздел <FiArrowRight /></button></article></section></div>;
}
