import { type FormEvent, useEffect, useState } from 'react';
import { api, formatPrice, type AuditLog, type PlatformStatistics, type Product, type Promocode, type Review, type Store, type UserProfile } from '@makon/api-client';

type Props = { onToast: (message: string) => void };
const statisticLabels: Record<keyof PlatformStatistics, string> = {
  users: 'Пользователи', sellers: 'Продавцы', stores: 'Магазины',
  products: 'Товары', orders: 'Заказы', confirmedOrders: 'Подтверждены', reviews: 'Отзывы',
};

export function AdminPanel({ onToast }: Props) {
  const [stats, setStats] = useState<PlatformStatistics | null>(null);
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [promocodes, setPromocodes] = useState<Promocode[]>([]);
  const [audit, setAudit] = useState<AuditLog[]>([]);
  const [busy, setBusy] = useState(false);
  const [promo, setPromo] = useState({ code: '', discountType: 'PERCENT' as 'PERCENT' | 'FIXED', discountValue: '', minimumAmount: '', maxUses: '' });

  async function load() {
    try {
      const [statistics, userPage, storePage, productPage, reviewPage, promoPage, auditPage] = await Promise.all([
        api.adminStatistics(), api.adminUsers(), api.adminPendingStores(), api.adminProducts(),
        api.adminPendingReviews(), api.adminPromocodes(), api.adminAudit(),
      ]);
      setStats(statistics); setUsers(userPage.content); setStores(storePage.content);
      setProducts(productPage.content); setReviews(reviewPage.content);
      setPromocodes(promoPage.content); setAudit(auditPage.content);
    } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось загрузить панель администратора'); }
  }
  useEffect(() => { void load(); }, []);

  async function run(action: () => Promise<unknown>, success: string) {
    setBusy(true);
    try { await action(); await load(); onToast(success); return true; }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось выполнить действие'); return false; }
    finally { setBusy(false); }
  }
  function reason() { return window.prompt('Укажите причину'); }
  async function createPromo(event: FormEvent) {
    event.preventDefault();
    const created = await run(() => api.adminCreatePromocode({
      code: promo.code.trim(), discountType: promo.discountType,
      discountValue: Number(promo.discountValue),
      minimumAmount: promo.minimumAmount ? Number(promo.minimumAmount) : undefined,
      maxUses: promo.maxUses ? Number(promo.maxUses) : undefined,
    }), 'Промокод создан');
    if (created) setPromo({ code: '', discountType: 'PERCENT', discountValue: '', minimumAmount: '', maxUses: '' });
  }

  return <div className="dashboard-panel admin-panel">
    <section className="moderation-hero"><div><p>Администрирование</p><h2>Платформа</h2><span>Заявки, пользователи, товары, отзывы и промокоды.</span></div><button disabled={busy} onClick={load}>Обновить</button></section>
    {stats && <section className="admin-stats">{(Object.keys(statisticLabels) as (keyof PlatformStatistics)[]).map((key) => <div key={key}><b>{stats[key]}</b><small>{statisticLabels[key]}</small></div>)}</section>}

    <section className="seller-queue"><header><h3>Магазины на проверке · {stores.length}</h3></header>
      {stores.map((store) => <article key={store.id}><div><b>{store.name}</b><span>{store.slug}</span></div><button disabled={busy} onClick={() => run(() => api.adminModerateStore(store.id, 'approve'), 'Магазин одобрен')}>Одобрить</button><button disabled={busy} onClick={() => { const text = reason(); if (text?.trim()) void run(() => api.adminModerateStore(store.id, 'reject', text.trim()), 'Магазин отклонён'); }}>Отклонить</button></article>)}
      {!stores.length && <p>Новых магазинов нет.</p>}
    </section>

    <section className="seller-queue"><header><h3>Отзывы на проверке · {reviews.length}</h3></header>
      {reviews.map((review) => <article key={review.id}><div><b>{review.username} · {'★'.repeat(review.rating)}</b><span>{review.comment || 'Без текста'}</span></div><button disabled={busy} onClick={() => run(() => api.adminModerateReview(review.id, 'approve'), 'Отзыв одобрен')}>Одобрить</button><button disabled={busy} onClick={() => run(() => api.adminModerateReview(review.id, 'reject'), 'Отзыв отклонён')}>Отклонить</button></article>)}
      {!reviews.length && <p>Новых отзывов нет.</p>}
    </section>

    <section className="seller-queue"><header><h3>Пользователи · первые {users.length}</h3></header>
      {users.map((user) => <article key={user.id}><div><b>{user.username}</b><span>{user.fullName || user.id} · {user.status}</span></div><button disabled={busy} onClick={() => run(() => api.adminUserStatus(user.id, user.status === 'BLOCKED' ? 'unblock' : 'block'), 'Статус пользователя обновлён')}>{user.status === 'BLOCKED' ? 'Разблокировать' : 'Заблокировать'}</button></article>)}
    </section>

    <section className="seller-queue"><header><h3>Товары · первые {products.length}</h3></header>
      {products.map((product) => <article key={product.id}><div><b>{product.name}</b><span>{product.storeSlug} · {formatPrice(product.price)}</span></div><button disabled={busy} onClick={() => { if (product.moderationBlocked) void run(() => api.adminModerateProduct(product.id, 'unblock'), 'Товар разблокирован'); else { const text = reason(); if (text?.trim()) void run(() => api.adminModerateProduct(product.id, 'block', text.trim()), 'Товар заблокирован'); } }}>{product.moderationBlocked ? 'Разблокировать' : 'Заблокировать'}</button></article>)}
    </section>

    <section className="seller-queue"><header><h3>Промокоды</h3></header>
      <form onSubmit={createPromo}><label>Код<input required pattern="[A-Za-z0-9_-]{3,40}" value={promo.code} onChange={(event) => setPromo({ ...promo, code: event.target.value })} /></label><label>Тип<select value={promo.discountType} onChange={(event) => setPromo({ ...promo, discountType: event.target.value as 'PERCENT' | 'FIXED' })}><option value="PERCENT">Проценты</option><option value="FIXED">Сумма</option></select></label><label>Скидка<input required type="number" min="0.01" step="0.01" value={promo.discountValue} onChange={(event) => setPromo({ ...promo, discountValue: event.target.value })} /></label><label>Минимальная сумма<input type="number" min="0" value={promo.minimumAmount} onChange={(event) => setPromo({ ...promo, minimumAmount: event.target.value })} /></label><label>Лимит применений<input type="number" min="1" value={promo.maxUses} onChange={(event) => setPromo({ ...promo, maxUses: event.target.value })} /></label><button disabled={busy}>Создать</button></form>
      {promocodes.map((item) => <article key={item.id}><div><b>{item.code}</b><span>{item.discountValue}{item.discountType === 'PERCENT' ? '%' : ' сум'} · использован {item.usedCount} раз · {item.active ? 'активен' : 'выключен'}</span></div>{item.active && <button disabled={busy} onClick={() => run(() => api.adminDeactivatePromocode(item.id), 'Промокод выключен')}>Выключить</button>}</article>)}
    </section>

    <section className="seller-queue"><header><h3>Журнал действий · последние {audit.length}</h3></header>
      {audit.map((entry) => <article key={entry.id}><div><b>{entry.action}</b><span>{entry.actor} · {entry.targetType} · {new Date(entry.createdAt).toLocaleString('ru-RU')}</span></div></article>)}
    </section>
  </div>;
}
