'use client';

import { type FormEvent, useEffect, useState } from 'react';
import { FiExternalLink, FiLogOut, FiPackage, FiRefreshCw, FiX } from 'react-icons/fi';
import { api, formatPrice, type Order, type UserProfile } from '@makon/api-client';

type Props = { profile: UserProfile; refreshKey: number; onProfile: (profile: UserProfile) => void; onClose: () => void; onLogout: () => void; onToast: (message: string) => void };

export function AccountDrawer({ profile, refreshKey, onProfile, onClose, onLogout, onToast }: Props) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [fullName, setFullName] = useState(profile.fullName ?? '');
  const [phone, setPhone] = useState(profile.phone ?? '');
  const [savingProfile, setSavingProfile] = useState(false);
  const [historyId, setHistoryId] = useState<string | null>(null);
  const [history, setHistory] = useState<Awaited<ReturnType<typeof api.orderHistory>>>([]);
  async function load() {
    setLoading(true);
    try { setOrders((await api.orders()).content); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось загрузить заказы'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [refreshKey]);
  async function cancel(orderId: string) {
    try { await api.cancelOrder(orderId); await load(); onToast('Заказ отменён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось отменить заказ'); }
  }
  async function saveProfile(event: FormEvent) {
    event.preventDefault(); setSavingProfile(true);
    try { onProfile(await api.updateProfile({ fullName: fullName.trim(), phone: phone.trim() })); onToast('Профиль обновлён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить профиль'); }
    finally { setSavingProfile(false); }
  }
  async function toggleHistory(orderId: string) {
    if (historyId === orderId) { setHistoryId(null); return; }
    try { setHistory(await api.orderHistory(orderId)); setHistoryId(orderId); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось загрузить историю заказа'); }
  }
  return <div className="drawer-layer" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <aside className="side-drawer side-drawer--account" aria-label="Личный кабинет">
      <header><div><p className="eyebrow">Личный кабинет</p><h2>{profile.username}</h2></div><button className="icon-button" onClick={onClose}><FiX /></button></header>
      <div className="account-actions"><a href="http://localhost:4200" target="_blank" rel="noreferrer">Кабинет продавца и администратора <FiExternalLink /></a><button onClick={load}><FiRefreshCw />Обновить заказы</button></div>
      <form className="account-profile" onSubmit={saveProfile}><h3>Мой профиль</h3><label>Имя<input maxLength={150} value={fullName} onChange={(event) => setFullName(event.target.value)} /></label><label>Телефон<input maxLength={32} value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="+998…" /></label><button disabled={savingProfile}>{savingProfile ? 'Сохраняем…' : 'Сохранить профиль'}</button></form>
      <section className="orders-list">
        <div className="orders-list__title"><FiPackage /><h3>Мои заказы</h3></div>
        {loading && <div className="panel-loading">Загружаем историю…</div>}
        {!loading && orders.map((order) => <article className="order-card" key={order.orderId}>
          <header><div><span>№ {order.orderId.slice(0, 8)}</span><b>{order.status}</b></div><strong>{formatPrice(order.totalAmount)}</strong></header>
          <p>{new Date(order.createdAt).toLocaleString('ru-RU')}</p>
          {order.delivery && <p>Доставка: {order.delivery.region}, {order.delivery.city}, {order.delivery.addressLine}. Получатель: {order.delivery.recipientName}, {order.delivery.recipientPhone}</p>}
          {order.sellerOrders?.flatMap((part) => part.items || []).map((item) => <div className="order-item" key={item.itemId}><span>{item.productName} × {item.quantity}</span><b>{formatPrice(item.subtotal)}</b></div>)}
          {order.discountAmount > 0 && <p>Скидка: −{formatPrice(order.discountAmount)} {order.promocode && `(${order.promocode})`}</p>}
          <button onClick={() => toggleHistory(order.orderId)}>{historyId === order.orderId ? 'Скрыть историю' : 'История статусов'}</button>
          {historyId === order.orderId && <ol>{history.map((entry) => <li key={entry.id}>{new Date(entry.createdAt).toLocaleString('ru-RU')}: {entry.newStatus}{entry.reason ? ` — ${entry.reason}` : ''}</li>)}</ol>}
          {order.status === 'PENDING' && <button className="action-danger" onClick={() => cancel(order.orderId)}>Отменить заказ</button>}
        </article>)}
        {!loading && !orders.length && <div className="drawer-empty"><FiPackage /><h3>Заказов пока нет</h3><p>После оформления они появятся здесь.</p></div>}
      </section>
      <button className="logout" onClick={onLogout}><FiLogOut />Выйти из аккаунта</button>
    </aside>
  </div>;
}
