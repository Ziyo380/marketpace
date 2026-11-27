'use client';

import { useEffect, useState } from 'react';
import { api, shipmentLabels, type ShipmentHistory } from '../../api-client/src';
import { useLiveRefresh } from './use-live-refresh';

type Props = { sellerOrderId?: string; orderId?: string; shipmentId?: string; refreshKey?: string };
const actors = { SELLER: 'Продавец', SYSTEM: 'Система', ADMIN: 'Администратор' };

export function ShipmentHistoryTimeline({ sellerOrderId, orderId, shipmentId, refreshKey }: Props) {
  const [events, setEvents] = useState<ShipmentHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  useLiveRefresh(() => { if (!loading) setRevision((v) => v + 1); });
  useEffect(() => {
    let active = true;
    setLoading(true); setError('');
    const request = sellerOrderId
      ? api.sellerShipmentHistory(sellerOrderId)
      : orderId && shipmentId
        ? api.customerShipmentHistory(orderId, shipmentId)
        : Promise.reject(new Error('Не выбрана доставка'));
    request.then((result) => { if (active) setEvents(result); })
      .catch((error) => { if (active) setError(error instanceof Error ? error.message : 'История недоступна'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [sellerOrderId, orderId, shipmentId, refreshKey, revision]);
  return <section className="shipment-history" aria-label="История доставки" aria-busy={loading}>
    <header><h4>История доставки</h4><button type="button" disabled={loading} onClick={() => setRevision((v) => v + 1)}>Обновить историю</button></header>
    {loading && events.length === 0 ? <p role="status">Загружаем события…</p> : error ? <p role="alert">{error}</p> : events.length === 0 ? <p>Событий пока нет.</p> : <ol>
      {events.map((event) => <li key={event.id} data-status={event.toStatus}>
        <time dateTime={event.changedAt}>{new Date(event.changedAt).toLocaleString('ru-RU')}</time>
        <strong>{shipmentLabels[event.toStatus]}</strong>
        {event.fromStatus && <span>Предыдущий статус: {shipmentLabels[event.fromStatus]}</span>}
        <span>{actors[event.actorType]}{event.actorUsername ? ' · ' + event.actorUsername : ''}</span>
        {event.details && <p>{event.details}</p>}
      </li>)}
    </ol>}
  </section>;
}
