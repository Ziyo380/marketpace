import { useEffect, useState, type FormEvent } from 'react';
import { api, ApiError, shipmentLabels, type Shipment } from '@makon/api-client';
import { ShipmentHistoryTimeline, useLiveRefresh } from '@makon/shared-ui';

export function ShipmentTools({ sellerOrderId }: { sellerOrderId: string }) {
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [carrierName, setCarrierName] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [failureReason, setFailureReason] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyRevision, setHistoryRevision] = useState(0);
  async function load() {
    setLoading(true); setError('');
    try { setShipment(await api.sellerShipment(sellerOrderId)); setHistoryRevision((v) => v + 1); }
    catch (error) {
      if (error instanceof ApiError && error.status === 404) setShipment(null);
      else setError(error instanceof Error ? error.message : 'Не удалось загрузить доставку');
    } finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [sellerOrderId]);
  useLiveRefresh(async () => {
    if (loading || busy) return;
    const updated = await api.sellerShipment(sellerOrderId);
    if (updated.status !== shipment?.status) setHistoryRevision((v) => v + 1);
    setShipment(updated);
  });
  async function change(action: () => Promise<Shipment>) {
    setBusy(true); setError('');
    try { setShipment(await action()); setConfirming(false); setHistoryRevision((v) => v + 1); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось обновить доставку'); }
    finally { setBusy(false); }
  }
  function dispatch(event: FormEvent) {
    event.preventDefault();
    if (trackingNumber.trim() && !carrierName.trim()) { setError('Укажите перевозчика для трек-номера'); return; }
    void change(() => api.dispatchShipment(sellerOrderId, { carrierName: carrierName.trim() || undefined, trackingNumber: trackingNumber.trim() || undefined }));
  }
  return <section className="shipment-tools" aria-label="Управление доставкой">
    <header><h3>Доставка</h3><button disabled={busy || loading} onClick={load}>Обновить</button></header>
    {error && <p role="alert">{error}</p>}
    {loading ? <p>Загружаем доставку…</p> : !shipment ? <p>Доставка пока не создана. Обновите после подтверждения заказа.</p> : <>
      <strong className="shipment-status" data-status={shipment.status}>{shipmentLabels[shipment.status]}</strong>
      {shipment.carrierName && <p>Перевозчик: {shipment.carrierName}</p>}
      {shipment.trackingNumber && <p>Трек-номер: {shipment.trackingNumber}</p>}
      {shipment.shippedAt && <p>Отправлено: {new Date(shipment.shippedAt).toLocaleString('ru-RU')}</p>}
      {shipment.deliveredAt && <p>Доставлено: {new Date(shipment.deliveredAt).toLocaleString('ru-RU')}</p>}
      {shipment.failureReason && <p>Причина: {shipment.failureReason}</p>}
      <details onToggle={(e) => setHistoryOpen(e.currentTarget.open)}><summary>История доставки</summary>{historyOpen && <ShipmentHistoryTimeline sellerOrderId={sellerOrderId} refreshKey={String(historyRevision)} />}</details>
      {shipment.status === 'CREATED' && <button disabled={busy} onClick={() => change(() => api.prepareShipment(sellerOrderId, 'PACKING'))}>Начать сборку</button>}
      {shipment.status === 'PACKING' && <button disabled={busy} onClick={() => change(() => api.prepareShipment(sellerOrderId, 'READY_FOR_DISPATCH'))}>Готово к отправке</button>}
      {shipment.status === 'READY_FOR_DISPATCH' && <form onSubmit={dispatch}><label>Перевозчик (необязательно)<input maxLength={100} value={carrierName} onChange={(e) => setCarrierName(e.target.value)} /></label><label>Трек-номер<input maxLength={128} value={trackingNumber} onChange={(e) => setTrackingNumber(e.target.value)} /></label><button disabled={busy}>Передать в доставку</button></form>}
      {shipment.status === 'IN_TRANSIT' && <>
        <button disabled={busy} onClick={() => setConfirming(true)}>Подтвердить вручение</button>
        {confirming && <div><p>Получатель действительно получил посылку?</p><button disabled={busy} onClick={() => change(() => api.confirmDelivery(sellerOrderId))}>Да, доставлена</button><button disabled={busy} onClick={() => setConfirming(false)}>Назад</button></div>}
        <form onSubmit={(e) => { e.preventDefault(); if (failureReason.trim()) void change(() => api.failDelivery(sellerOrderId, failureReason.trim())); }}><label>Причина неудачной доставки<textarea required maxLength={255} value={failureReason} onChange={(e) => setFailureReason(e.target.value)} /></label><button disabled={busy || !failureReason.trim()}>Отметить неудачную доставку</button></form>
      </>}
    </>}
  </section>;
}
