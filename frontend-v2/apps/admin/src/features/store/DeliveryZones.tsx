import { useEffect, useState, type FormEvent } from 'react';
import { api, formatPrice, type DeliveryRegion, type StoreDeliveryZone } from '@makon/api-client';

export function DeliveryZones({ storeId }: { storeId: string }) {
  const [zones, setZones] = useState<StoreDeliveryZone[]>([]);
  const [regions, setRegions] = useState<DeliveryRegion[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ regionCode: '', deliveryFee: '', minDeliveryDays: '1', maxDeliveryDays: '3' });
  async function load() {
    setLoading(true); setError('');
    try { const [zones, regions] = await Promise.all([api.storeDeliveryZones(storeId), api.deliveryRegions()]); setZones(zones); setRegions(regions); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось загрузить зоны'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [storeId]);
  async function save(event: FormEvent) {
    event.preventDefault();
    if (Number(form.minDeliveryDays) > Number(form.maxDeliveryDays)) { setError('Минимальный срок не должен превышать максимальный'); return; }
    setBusy(true); setError('');
    try { await api.createDeliveryZone(storeId, { regionCode: form.regionCode, deliveryFee: Number(form.deliveryFee), minDeliveryDays: Number(form.minDeliveryDays), maxDeliveryDays: Number(form.maxDeliveryDays) }); setForm({ regionCode: '', deliveryFee: '', minDeliveryDays: '1', maxDeliveryDays: '3' }); await load(); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось добавить зону'); }
    finally { setBusy(false); }
  }
  async function toggle(zone: StoreDeliveryZone) {
    setBusy(true); setError('');
    try { const updated = await api.toggleDeliveryZone(storeId, zone.regionCode, !zone.active); setZones((zones) => zones.map((z) => z.id === updated.id ? updated : z)); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось изменить зону'); }
    finally { setBusy(false); }
  }
  return <section className="form-card delivery-zones"><header><div><p>Доставка магазина</p><h2>Регионы, стоимость и сроки</h2></div><button disabled={loading || busy} onClick={load}>Обновить</button></header>
    <p>Покупатель сможет оформить заказ только в активном регионе доставки каждого магазина.</p>
    {error && <p role="alert">{error}</p>}
    {loading ? <p>Загружаем…</p> : <>{zones.map((zone) => <article key={zone.id}><div><b>{zone.regionName}</b><p>{formatPrice(zone.deliveryFee)} · {zone.minDeliveryDays}–{zone.maxDeliveryDays} дн.</p></div><button disabled={busy} onClick={() => toggle(zone)}>{zone.active ? 'Отключить' : 'Включить'}</button></article>)}{!zones.length && <p>Зон пока нет. Добавьте первый регион.</p>}</>}
    <form onSubmit={save}><label>Регион<select required value={form.regionCode} onChange={(e) => setForm({ ...form, regionCode: e.target.value })}><option value="">Выберите регион</option>{regions.filter((r) => !zones.some((z) => z.regionCode === r.code)).map((r) => <option value={r.code} key={r.code}>{r.name}</option>)}</select></label><label>Стоимость, сум<input required type="number" min="0" step="0.01" value={form.deliveryFee} onChange={(e) => setForm({ ...form, deliveryFee: e.target.value })} /></label><label>От, дней<input required type="number" min="1" step="1" value={form.minDeliveryDays} onChange={(e) => setForm({ ...form, minDeliveryDays: e.target.value })} /></label><label>До, дней<input required type="number" min={form.minDeliveryDays || 1} step="1" value={form.maxDeliveryDays} onChange={(e) => setForm({ ...form, maxDeliveryDays: e.target.value })} /></label><button className="primary" disabled={busy || loading}>Добавить регион</button></form>
  </section>;
}
