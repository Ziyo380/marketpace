import { useEffect, useState } from 'react';
import { FiCheck, FiClock, FiRefreshCw, FiShoppingBag, FiX } from 'react-icons/fi';
import { api, formatPrice, type Seller, type SellerOrder } from '@makon/api-client';

import { ShipmentTools } from './ShipmentTools';
import { useLiveRefresh } from '@makon/shared-ui';

export function SellerOrdersPanel({ seller, onToast }: { seller: Seller | null; onToast: (message: string) => void }) {
  const [orders, setOrders] = useState<SellerOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<string | null>(null);
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [history, setHistory] = useState<Record<string, Awaited<ReturnType<typeof api.sellerOrderHistory>>>>({});
  const [historyBusy, setHistoryBusy] = useState<string | null>(null);
  async function showHistory(id: string) {
    setHistoryBusy(id);
    try { const result = await api.sellerOrderHistory(id); setHistory((current) => ({ ...current, [id]: result })); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось загрузить историю'); }
    finally { setHistoryBusy(null); }
  }

  async function load(nextPage = 0) {
    if (!seller) return;
    if (nextPage === 0) setLoading(true); else setLoadingMore(true);
    setError('');
    try {
      const result = await api.sellerOrders(nextPage);
      setOrders((current) => nextPage === 0 ? result.content : [...current, ...result.content]);
      setPage(result.number); setHasMore(!result.last);
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'Не удалось загрузить заказы'); }
    finally { setLoading(false); setLoadingMore(false); }
  }

  useEffect(() => { void load(); }, [seller?.id]);
  useLiveRefresh(async () => {
    if (loading || loadingMore || busyId) return;
    const pages = await Promise.all(Array.from({ length: page + 1 }, (_, index) => api.sellerOrders(index)));
    setOrders(pages.flatMap((result) => result.content)); setHasMore(!pages[pages.length - 1].last);
  }, !!seller);

  async function confirm(id: string) {
    setBusyId(id);
    try { await api.confirmSellerOrder(id); await load(); onToast('Заказ подтверждён'); }
    catch (caught) { onToast(caught instanceof Error ? caught.message : 'Не удалось подтвердить заказ'); }
    finally { setBusyId(null); }
  }

  async function cancel(id: string) {
    if (!reason.trim()) return;
    setBusyId(id);
    try { await api.cancelSellerOrder(id, reason.trim()); setCancelId(null); setReason(''); await load(); onToast('Заказ отменён'); }
    catch (caught) { onToast(caught instanceof Error ? caught.message : 'Не удалось отменить заказ'); }
    finally { setBusyId(null); }
  }

  return <div className="dashboard-panel seller-orders"><section className="seller-orders__heading"><div><p>Заказы магазина</p><h2>Обработка заказов</h2><span>Подтверждайте или отменяйте только заказы своего магазина.</span></div><button onClick={() => load()} disabled={loading}><FiRefreshCw />Обновить</button></section>
    {!seller && <div className="empty-workspace"><FiShoppingBag /><h2>Нет профиля продавца</h2><p>Подайте заявку и дождитесь одобрения, чтобы получать заказы.</p></div>}
    {seller && error && <div className="seller-orders__error">{error}<button onClick={() => load()}>Повторить</button></div>}
    {seller && loading && <div className="panel-loading">Загружаем заказы…</div>}
    {seller && !loading && !error && orders.map((order) => <article className="seller-order-card" key={order.sellerOrderId}>
      <header><div><FiClock /><span>№ {order.orderId.slice(0, 8)}</span><em data-status={order.status}>{order.status}</em></div><strong>{formatPrice(order.payableAmount ?? order.totalAmount)}</strong></header>
      {order.delivery && <p>Получатель: {order.delivery.recipientName}, {order.delivery.recipientPhone}. Адрес: {order.delivery.region}, {order.delivery.city}, {order.delivery.addressLine}</p>}
      <div className="seller-order-card__items">{order.items?.map((item) => <div key={item.itemId}><span>{item.productName} × {item.quantity}</span><b>{formatPrice(item.subtotal)}</b></div>)}</div>
      <div className="order-pricing"><p>Товары: {formatPrice(order.totalAmount)}</p><p>Доставка: {order.deliveryFee == null ? 'Нет данных' : formatPrice(order.deliveryFee)}</p>{order.discountAmount != null && order.discountAmount > 0 && <p>Скидка: −{formatPrice(order.discountAmount)}</p>}{order.minDeliveryDays != null && <p>Срок: {order.minDeliveryDays}–{order.maxDeliveryDays} дн. после отправки</p>}{order.cancellationReson && <p>Причина отмены: {order.cancellationReson}</p>}</div>
      {order.status === 'CONFIRMED' && <ShipmentTools sellerOrderId={order.sellerOrderId} />}
      <details><summary>История заказа</summary><button disabled={historyBusy === order.sellerOrderId} onClick={() => showHistory(order.sellerOrderId)}>Загрузить историю</button>{history[order.sellerOrderId]?.map((entry) => <p key={entry.id}>{new Date(entry.createdAt).toLocaleString('ru-RU')}: {entry.previousStatus} → {entry.newStatus}{entry.reason && ' · ' + entry.reason}</p>)}{history[order.sellerOrderId]?.length === 0 && <p>История пуста</p>}</details>
      {order.status === 'PENDING' && <footer><button disabled={busyId === order.sellerOrderId} onClick={() => confirm(order.sellerOrderId)}><FiCheck />Подтвердить</button><button disabled={busyId === order.sellerOrderId} onClick={() => { setCancelId(order.sellerOrderId); setReason(''); }}><FiX />Отменить</button></footer>}
      {cancelId === order.sellerOrderId && <div className="seller-order-card__cancel"><label>Причина отмены<textarea required value={reason} onChange={(event) => setReason(event.target.value)} /></label><button disabled={!reason.trim() || busyId === order.sellerOrderId} onClick={() => cancel(order.sellerOrderId)}>Подтвердить отмену</button><button onClick={() => setCancelId(null)}>Назад</button></div>}
    </article>)}
    {seller && !loading && !error && !orders.length && <div className="empty-workspace"><FiShoppingBag /><h2>Заказов пока нет</h2><p>Когда покупатель оформит заказ с вашим товаром, он появится здесь.</p></div>}
    {seller && hasMore && <button className="seller-orders__more" disabled={loadingMore} onClick={() => load(page + 1)}>{loadingMore ? 'Загружаем…' : 'Показать ещё'}</button>}
  </div>;
}
