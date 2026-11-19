'use client';

import { type FormEvent, useEffect, useState } from 'react';
import { FiExternalLink, FiLogOut, FiPackage, FiRefreshCw, FiX } from 'react-icons/fi';
import { api, formatPrice, orderStatusLabels, type Order, type UserProfile } from '@makon/api-client';

import { OrderShipments } from './OrderShipments';
import { MyReviews } from './MyReviews';
import { useLiveRefresh } from '@makon/shared-ui';

type Props = { profile: UserProfile; refreshKey: number; onProfile: (profile: UserProfile) => void; onClose: () => void; onLogout: () => void; onToast: (message: string) => void };

export function AccountDrawer({ profile, refreshKey, onProfile, onClose, onLogout, onToast }: Props) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [busyOrder, setBusyOrder] = useState<string | null>(null);
  const [expandedShipments, setExpandedShipments] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [fullName, setFullName] = useState(profile.fullName ?? '');
  const [phone, setPhone] = useState(profile.phone ?? '');
  const [savingProfile, setSavingProfile] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileSaved, setProfileSaved] = useState(false);
  const [historyId, setHistoryId] = useState<string | null>(null);
  const [history, setHistory] = useState<Awaited<ReturnType<typeof api.orderHistory>>>([]);
  async function load(nextPage = 0) {
    setLoading(true); setLoadError('');
    try { const result = await api.orders(nextPage); setOrders((current) => nextPage === 0 ? result.content : [...current, ...result.content]); setPage(result.number); setHasMore(!result.last); }
    catch (error) { setLoadError(error instanceof Error ? error.message : 'Не удалось загрузить заказы'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [refreshKey]);
  useEffect(() => {
    if (!editingProfile) { setFullName(profile.fullName ?? ''); setPhone(profile.phone ?? ''); }
  }, [profile.fullName, profile.phone, editingProfile]);
  useLiveRefresh(async () => {
    if (loading || busyOrder) return;
    const pages = await Promise.all(Array.from({ length: page + 1 }, (_, index) => api.orders(index)));
    setOrders(pages.flatMap((result) => result.content));
    setHasMore(!pages[pages.length - 1].last);
    if (!editingProfile && !savingProfile) onProfile(await api.profile());
  });
  async function cancel(orderId: string) {
    setBusyOrder(orderId);
    try { await api.cancelOrder(orderId); await load(); onToast('Заказ отменён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось отменить заказ'); }
    finally { setBusyOrder(null); }
  }
  async function saveProfile(event: FormEvent) {
    event.preventDefault(); setSavingProfile(true); setProfileError('');
    try {
      const saved = await api.updateProfile({ fullName: fullName.trim(), phone: phone.trim() });
      onProfile(saved); setFullName(saved.fullName ?? ''); setPhone(saved.phone ?? '');
      setEditingProfile(false); setProfileSaved(true); onToast('Профиль обновлён');
    }
    catch (error) { setProfileError(error instanceof Error ? error.message : 'Не удалось обновить профиль'); }
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
      <div className="account-actions"><a href="http://localhost:4200" target="_blank" rel="noreferrer">Кабинет продавца и администратора <FiExternalLink /></a><button onClick={() => load()}><FiRefreshCw />Обновить заказы</button></div>
      {editingProfile ? <form className="account-profile" onSubmit={saveProfile}><h3>Редактирование профиля</h3><label>Имя<input disabled={savingProfile} maxLength={100} value={fullName} onChange={(event) => setFullName(event.target.value)} /></label><label>Телефон<input disabled={savingProfile} maxLength={30} value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="+998…" /></label>{profileError && <p className="full" role="alert">{profileError}</p>}<button disabled={savingProfile}>{savingProfile ? 'Сохраняем…' : 'Сохранить профиль'}</button><button type="button" disabled={savingProfile} onClick={() => setEditingProfile(false)}>Отмена</button></form> : <section className="account-profile-summary" aria-label="Мой профиль"><h3>Мой профиль</h3><p>Имя: <strong>{profile.fullName || 'Не указано'}</strong></p><p>Телефон: <strong>{profile.phone || 'Не указан'}</strong></p>{profileSaved && <p role="status">Изменения сохранены</p>}<button onClick={() => { setProfileSaved(false); setProfileError(''); setEditingProfile(true); }}>Редактировать профиль</button></section>}
      <section className="orders-list">
        <div className="orders-list__title"><FiPackage /><h3>Мои заказы</h3></div>
        {loading && <div className="panel-loading">Загружаем историю…</div>}
        {loadError && <p role="alert">{loadError}</p>}
        {orders.map((order) => <article className="order-card" key={order.orderId}>
          <header><div><span>№ {order.orderId.slice(0, 8)}</span><b>{orderStatusLabels[order.status] ?? order.status}</b></div><strong>{formatPrice(order.currentTotalAmount ?? order.totalAmount)}</strong></header>
          <p>{new Date(order.createdAt).toLocaleString('ru-RU')}</p>
          {order.delivery && <p>Доставка: {order.delivery.region}, {order.delivery.city}, {order.delivery.addressLine}. Получатель: {order.delivery.recipientName}, {order.delivery.recipientPhone}</p>}
          <div className="order-pricing"><p>Товары: {formatPrice(order.subtotalAmount)}</p><p>Доставка: {formatPrice(order.deliveryTotalAmount)}</p>{order.currentTotalAmount != null && order.currentTotalAmount !== order.totalAmount && <p>При оформлении: {formatPrice(order.totalAmount)}. Текущая сумма учитывает отменённые части.</p>}</div>
          {order.sellerOrders?.map((part) => <section className="order-part" key={part.sellerOrderId}><h4>Часть № {part.sellerOrderId.slice(0, 8)} · {part.status}</h4>{part.items.map((item) => <div className="order-item" key={item.itemId}><span>{item.productName} × {item.quantity}</span><b>{formatPrice(item.subtotal)}</b></div>)}<p>Доставка: {part.deliveryFee == null ? 'Нет данных' : formatPrice(part.deliveryFee)}{part.minDeliveryDays != null && ` · ${part.minDeliveryDays}–${part.maxDeliveryDays} дн.`}</p>{part.payableAmount != null && <p>Сумма части: {formatPrice(part.payableAmount)}</p>}{part.cancellationReson && <p>Причина отмены: {part.cancellationReson}</p>}</section>)}
          <details onToggle={(e) => { const open = e.currentTarget.open; setExpandedShipments((current) => { const next = new Set(current); if (open) next.add(order.orderId); else next.delete(order.orderId); return next; }); }}><summary>Отследить доставки</summary>{expandedShipments.has(order.orderId) && <OrderShipments orderId={order.orderId} parts={order.sellerOrders ?? []} />}</details>
          {order.discountAmount > 0 && <p>Скидка: −{formatPrice(order.discountAmount)} {order.promocode && `(${order.promocode})`}</p>}
          <button onClick={() => toggleHistory(order.orderId)}>{historyId === order.orderId ? 'Скрыть историю' : 'История статусов'}</button>
          {historyId === order.orderId && <ol>{history.map((entry) => <li key={entry.id}>{new Date(entry.createdAt).toLocaleString('ru-RU')}: {orderStatusLabels[entry.newStatus] ?? entry.newStatus}{entry.reason ? ` — ${entry.reason}` : ''}</li>)}</ol>}
          {order.status === 'PENDING' && <button className="action-danger" disabled={busyOrder === order.orderId} onClick={() => cancel(order.orderId)}>Отменить заказ</button>}
        </article>)}
        {hasMore && <button disabled={loading} onClick={() => load(page + 1)}>Показать ещё заказы</button>}
        {!loading && !loadError && !orders.length && <div className="drawer-empty"><FiPackage /><h3>Заказов пока нет</h3><p>После оформления они появятся здесь.</p></div>}
      </section>
      <MyReviews />
      <button className="logout" onClick={onLogout}><FiLogOut />Выйти из аккаунта</button>
    </aside>
  </div>;
}
