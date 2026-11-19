'use client';

import { useEffect, useState } from 'react';
import { FiMinus, FiPlus, FiShoppingBag, FiTrash2, FiX } from 'react-icons/fi';
import { api, formatPrice, type Cart, type DeliveryDetails, type DeliveryRegion, type Order } from '@makon/api-client';

type Props = { cart: Cart; onCart: (cart: Cart) => void; onClose: () => void; onOrder: (order: Order) => void; onToast: (message: string) => void };

export function CartDrawer({ cart, onCart, onClose, onOrder, onToast }: Props) {
  const [busy, setBusy] = useState(false);
  const [promocode, setPromocode] = useState('');
  const [discount, setDiscount] = useState<number | null>(null);
  const [delivery, setDelivery] = useState<DeliveryDetails>({ recipientName: '', recipientPhone: '', region: '', city: '', addressLine: '' });
  const [regions, setRegions] = useState<DeliveryRegion[]>([]);
  const [regionsError, setRegionsError] = useState(false);
  const checkoutStorageKey = 'makon_pending_checkout';
  useEffect(() => {
    let mounted = true;
    api.deliveryRegions()
      .then((result) => { if (mounted) setRegions(result); })
      .catch(() => { if (mounted) setRegionsError(true); });
    return () => { mounted = false; };
  }, []);
  function updateDelivery(field: keyof DeliveryDetails, value: string) {
    setDelivery((current) => ({ ...current, [field]: value }));
  }
  async function update(itemId: string, quantity: number) {
    setBusy(true);
    try { onCart(quantity <= 0 ? await api.removeCartItem(itemId) : await api.updateCartItem(itemId, quantity)); setDiscount(null); window.sessionStorage.removeItem(checkoutStorageKey); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить корзину'); }
    finally { setBusy(false); }
  }
  async function applyPromocode() {
    if (!promocode.trim()) { setDiscount(null); return; }
    setBusy(true);
    try { const preview = await api.previewPromocode(promocode.trim(), cart.totalPrice); setDiscount(preview.discountAmount); onToast(`Скидка ${formatPrice(preview.discountAmount)} применится к заказу`); }
    catch (error) { setDiscount(null); onToast(error instanceof Error ? error.message : 'Промокод не действует'); }
    finally { setBusy(false); }
  }
  async function checkout() {
    const normalizedDelivery = Object.fromEntries(
      Object.entries(delivery).map(([field, value]) => [field, value.trim()]),
    ) as DeliveryDetails;
    if (Object.values(normalizedDelivery).some((value) => !value)) {
      onToast('Заполните имя, телефон и адрес доставки');
      return;
    }
    setBusy(true);
    try {
      const signature = JSON.stringify({ items: cart.items.map(({ itemId, quantity }) => [itemId, quantity]).sort(), promocode: promocode.trim().toUpperCase(), delivery: normalizedDelivery });
      const saved = JSON.parse(window.sessionStorage.getItem(checkoutStorageKey) || 'null') as { signature: string; key: string } | null;
      const key = saved?.signature === signature ? saved.key : crypto.randomUUID();
      window.sessionStorage.setItem(checkoutStorageKey, JSON.stringify({ signature, key }));
      const order = await api.createOrder(key, normalizedDelivery, promocode.trim() || undefined);
      window.sessionStorage.removeItem(checkoutStorageKey);
      onOrder(order); onToast(`Заказ ${order.orderId.slice(0, 8)} создан`);
    }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось оформить заказ'); }
    finally { setBusy(false); }
  }
  return <div className="drawer-layer" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <aside className="side-drawer" aria-label="Корзина">
      <header><div><p className="eyebrow">Ваш выбор</p><h2>Корзина</h2></div><button className="icon-button" onClick={onClose} aria-label="Закрыть"><FiX /></button></header>
      <div className="cart-lines">
        {cart.items.map((item) => <article className="cart-line" key={item.itemId}>
          <div className="cart-line__mark">{item.productName.slice(0, 1)}</div><div className="cart-line__copy"><strong>{item.productName}</strong>{item.variantName && <small>{item.variantName}</small>}<span>{formatPrice(item.unitPrice)}</span></div>
          <div className="quantity"><button disabled={busy} onClick={() => update(item.itemId, item.quantity - 1)}><FiMinus /></button><b>{item.quantity}</b><button disabled={busy} onClick={() => update(item.itemId, item.quantity + 1)}><FiPlus /></button></div>
          <strong>{formatPrice(item.subtotal)}</strong><button className="remove" disabled={busy} onClick={() => update(item.itemId, 0)} aria-label="Удалить"><FiTrash2 /></button>
        </article>)}
        {!cart.items.length && <div className="drawer-empty"><FiShoppingBag /><h3>Здесь пока пусто</h3><p>Добавьте товары из каталога.</p></div>}
        {!!cart.items.length && <section className="checkout-details" aria-label="Доставка продавцом">
          <h3>Получатель и адрес доставки</h3>
          <p>Каждый магазин доставит свою часть заказа по этому адресу.</p>
          <label>Имя получателя<input autoComplete="name" maxLength={100} value={delivery.recipientName} onChange={(event) => updateDelivery('recipientName', event.target.value)} /></label>
          <label>Телефон<input type="tel" autoComplete="tel" maxLength={30} value={delivery.recipientPhone} onChange={(event) => updateDelivery('recipientPhone', event.target.value)} placeholder="+998 90 123 45 67" /></label>
          <label>Область<select value={delivery.region} onChange={(event) => updateDelivery('region', event.target.value)}>
            <option value="">Выберите область</option>
            {regions.map((region) => <option key={region.code} value={region.code}>{region.name}</option>)}
          </select></label>
          {regionsError && <p role="alert">Не удалось загрузить регионы. Попробуйте открыть корзину снова.</p>}
          <label>Город<input autoComplete="address-level2" maxLength={100} value={delivery.city} onChange={(event) => updateDelivery('city', event.target.value)} /></label>
          <label>Улица, дом, квартира<input autoComplete="street-address" maxLength={500} value={delivery.addressLine} onChange={(event) => updateDelivery('addressLine', event.target.value)} /></label>
        </section>}
      </div>
      <footer><div><span>Товаров</span><b>{cart.totalItems}</b></div><label>Промокод<input value={promocode} onChange={(event) => { setPromocode(event.target.value); setDiscount(null); }} placeholder="Введите код" /></label><button disabled={busy || !cart.items.length} onClick={applyPromocode}>Проверить промокод</button>{discount !== null && <div><span>Скидка</span><b>−{formatPrice(discount)}</b></div>}<div className="drawer-total"><span>Товары со скидкой</span><strong>{formatPrice(cart.totalPrice - (discount ?? 0))}</strong></div><button className="action-primary action-wide" disabled={busy || !cart.items.length || regionsError || !delivery.region} onClick={checkout}>{busy ? 'Обрабатываем…' : 'Оформить заказ'}</button><small>К стоимости товаров добавится доставка по тарифам магазинов. Точную сумму покажем после оформления. Оплата онлайн пока недоступна.</small></footer>
    </aside>
  </div>;
}
