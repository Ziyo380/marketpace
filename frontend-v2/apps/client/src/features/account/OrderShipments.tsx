'use client';

import { useEffect, useState } from 'react';
import { api, shipmentLabels, type SellerOrder, type Shipment } from '@makon/api-client';
import { ShipmentHistoryTimeline, useLiveRefresh } from '@makon/shared-ui';

export function OrderShipments({ orderId, parts }: { orderId: string; parts: SellerOrder[] }) {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedHistory, setExpandedHistory] = useState<Set<string>>(new Set());
  const [revision, setRevision] = useState(0);
  async function load() {
    setLoading(true); setError('');
    try { setShipments(await api.customerShipments(orderId)); setRevision((v) => v + 1); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось загрузить доставки'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [orderId]);
  useLiveRefresh(async () => {
    if (loading) return;
    const updated = await api.customerShipments(orderId);
    if (JSON.stringify(updated) !== JSON.stringify(shipments)) { setShipments(updated); setRevision((v) => v + 1); }
  });
  return <section className="order-shipments" aria-label="Посылки заказа"><header><h4>Посылки по магазинам</h4><button disabled={loading} onClick={load}>Обновить</button></header>
    {loading ? <p>Загружаем доставки…</p> : error ? <p role="alert">{error}</p> : <>
      {shipments.map((shipment) => { const part = parts.find((p) => p.sellerOrderId === shipment.sellerOrderId); return <article key={shipment.id}>
        <b>{shipmentLabels[shipment.status]}</b><p>Часть заказа № {shipment.sellerOrderId.slice(0, 8)}</p>
        <p>{part?.items.map((item) => item.productName).join(', ')}</p>
        {shipment.carrierName && <p>Перевозчик: {shipment.carrierName}</p>}
        {shipment.trackingNumber && <p>Трек-номер: <strong>{shipment.trackingNumber}</strong></p>}
        {shipment.shippedAt && <p>Отправлено {new Date(shipment.shippedAt).toLocaleString('ru-RU')}</p>}
        {shipment.deliveredAt && <p>Доставлено {new Date(shipment.deliveredAt).toLocaleString('ru-RU')}</p>}
        {shipment.failureReason && <p>Причина: {shipment.failureReason}</p>}
        <details onToggle={(e) => { const open = e.currentTarget.open; setExpandedHistory((current) => { const next = new Set(current); if (open) next.add(shipment.id); else next.delete(shipment.id); return next; }); }}><summary>История доставки</summary>{expandedHistory.has(shipment.id) && <ShipmentHistoryTimeline orderId={orderId} shipmentId={shipment.id} refreshKey={String(revision)} />}</details>
      </article>; })}
      {!shipments.length && <p>Посылок пока нет. Они появятся после подтверждения магазинами.</p>}
      {parts.filter((p) => p.status === 'PENDING').map((part) => <p key={part.sellerOrderId}>Часть № {part.sellerOrderId.slice(0, 8)} ожидает подтверждения магазина.</p>)}
    </>}
  </section>;
}
