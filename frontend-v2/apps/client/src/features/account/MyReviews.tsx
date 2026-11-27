'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { api, type Review } from '@makon/api-client';

export function MyReviews() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Review | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  async function load(nextPage = 0) {
    setLoading(true); setError('');
    try { const result = await api.myReviews(nextPage); setReviews((r) => nextPage === 0 ? result.content : [...r, ...result.content]); setPage(result.number); setHasMore(!result.last); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось загрузить отзывы'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, []);
  async function save(event: FormEvent) {
    event.preventDefault(); if (!editing) return; setBusy(true); setError('');
    try { await api.updateReview(editing.id, editing.rating, editing.comment?.trim() ?? ''); setEditing(null); await load(); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось сохранить отзыв'); }
    finally { setBusy(false); }
  }
  async function remove(id: string) {
    setBusy(true); setError('');
    try { await api.deleteReview(id); setDeleting(null); await load(); }
    catch (error) { setError(error instanceof Error ? error.message : 'Не удалось удалить отзыв'); }
    finally { setBusy(false); }
  }
  return <section className="my-reviews"><h3>Мои отзывы</h3><button disabled={busy || loading} onClick={() => load()}>Обновить отзывы</button>
    {error && <p role="alert">{error}</p>}{loading && <p>Загружаем…</p>}
    {reviews.map((review) => <article className="order-part" key={review.id}><b>{'★'.repeat(review.rating)} · {review.status}</b><p>{review.comment}</p><small>Товар № {review.productId.slice(0, 8)}</small><div><button disabled={busy} onClick={() => setEditing({ ...review })}>Изменить</button><button disabled={busy} onClick={() => setDeleting(review.id)}>Удалить</button></div>{deleting === review.id && <p>Удалить отзыв? <button disabled={busy} onClick={() => remove(review.id)}>Да</button><button disabled={busy} onClick={() => setDeleting(null)}>Назад</button></p>}</article>)}
    {editing && <form className="account-profile" onSubmit={save}><h4>Редактирование отзыва</h4><label>Оценка<select value={editing.rating} onChange={(e) => setEditing({ ...editing, rating: Number(e.target.value) })}>{[5,4,3,2,1].map((r) => <option key={r} value={r}>{r}</option>)}</select></label><label>Комментарий<textarea maxLength={2000} value={editing.comment ?? ''} onChange={(e) => setEditing({ ...editing, comment: e.target.value })} /></label><p>Изменённый отзыв снова проходит модерацию.</p><button disabled={busy}>Сохранить</button><button disabled={busy} type="button" onClick={() => setEditing(null)}>Отмена</button></form>}
    {!loading && !error && !reviews.length && <p>Вы ещё не оставляли отзывов.</p>}
    {hasMore && <button disabled={busy || loading} onClick={() => load(page + 1)}>Ещё отзывы</button>}
  </section>;
}
