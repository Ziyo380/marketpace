'use client';

import { type FormEvent, useEffect, useState } from 'react';
import { FiCheck, FiHeart, FiMinus, FiPlus, FiShoppingBag, FiX } from 'react-icons/fi';
import { api, formatPrice, type Product, type ProductVariant, type Review, type ProductRating, type ProductImage } from '@makon/api-client';
import { fallbackProductImage } from './product-images';

type Props = { product: Product; favorite: boolean; onFavorite: () => void; onAdd: (quantity: number, variantId?: string) => void; onToast: (message: string) => void; onClose: () => void };

export function ProductDialog({ product, favorite, onFavorite, onAdd, onToast, onClose }: Props) {
  const [quantity, setQuantity] = useState(1);
  const [images, setImages] = useState<ProductImage[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [variantId, setVariantId] = useState('');
  const [loadingVariants, setLoadingVariants] = useState(true);
  const [variantError, setVariantError] = useState('');
  const [reviews, setReviews] = useState<Review[]>([]);
  const [ratingSummary, setRatingSummary] = useState<ProductRating | null>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [sendingReview, setSendingReview] = useState(false);
  useEffect(() => {
    let current = true;
    setLoadingVariants(true);
    api.publicVariants(product.id).then((items) => {
      if (current) { setVariants(items); setVariantId(items.find((item) => item.active && item.availableQuantity > 0)?.id ?? items.find((item) => item.active)?.id ?? ''); setVariantError(''); }
    }).catch(() => { if (current) setVariantError('Не удалось загрузить варианты товара'); })
      .finally(() => { if (current) setLoadingVariants(false); });
    return () => { current = false; };
  }, [product.id]);
  useEffect(() => {
    let current = true;
    Promise.all([api.productReviews(product.id), api.productRating(product.id)])
      .then(([page, summary]) => { if (current) { setReviews(page.content); setRatingSummary(summary); } })
      .catch(() => { if (current) setReviews([]); });
    return () => { current = false; };
  }, [product.id]);
  useEffect(() => {
    let current = true; setImages([]); setSelectedImage(null);
    api.publicProductImages(product.storeSlug, product.slug).then((result) => { if (current) setImages(result); }).catch(() => { if (current) setImages([]); });
    return () => { current = false; };
  }, [product.storeSlug, product.slug]);
  async function submitReview(event: FormEvent) {
    event.preventDefault(); setSendingReview(true);
    try { await api.createReview(product.id, rating, comment.trim()); setComment(''); onToast('Отзыв отправлен на модерацию'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось отправить отзыв'); }
    finally { setSendingReview(false); }
  }
  const selectedVariant = variants.find((item) => item.id === variantId);
  const activeVariants = variants.filter((item) => item.active);
  const available = selectedVariant?.availableQuantity;
  const image = selectedImage || product.primaryImageUrl || fallbackProductImage(product);
  return <div className="overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="product-dialog" role="dialog" aria-modal="true" aria-labelledby="product-name">
      <button className="icon-button dialog-close" onClick={onClose} aria-label="Закрыть"><FiX /></button>
      <div className="product-dialog__image"><img src={image} alt={product.name} onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = fallbackProductImage(product); }} />{images.length > 1 && <div className="product-thumbnails">{images.map((image) => <button key={image.id} aria-label={image.originalFilename} aria-pressed={selectedImage === image.url} onClick={() => setSelectedImage(image.url)}><img src={image.url} alt="" /></button>)}</div>}</div>
      <div className="product-dialog__copy"><p className="eyebrow"><FiCheck />Опубликован · {product.storeSlug}</p><h2 id="product-name">{product.name}</h2><p>{product.description || 'Описание товара скоро появится.'}</p><strong>{formatPrice(selectedVariant?.price ?? product.price)}</strong>
        {loadingVariants && <p>Загружаем варианты…</p>}
        {variantError && <p role="alert">{variantError}</p>}
        {activeVariants.length > 0 && <label>Вариант<select value={variantId} onChange={(event) => { setVariantId(event.target.value); setQuantity(1); }}>{activeVariants.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.sku} · {formatPrice(item.price)} · доступно {item.availableQuantity}</option>)}</select></label>}
        {variants.length > 0 && !activeVariants.length && <p>Варианты этого товара пока не продаются.</p>}
        <div className="buy-controls"><div className="quantity"><button onClick={() => setQuantity((value) => Math.max(1, value - 1))}><FiMinus /></button><b>{quantity}</b><button onClick={() => setQuantity((value) => Math.min(available ?? Number.MAX_SAFE_INTEGER, value + 1))}><FiPlus /></button></div><button className="action-primary" disabled={loadingVariants || !!variantError || (variants.length > 0 && !variantId) || (available !== undefined && (available < quantity || available < 1))} onClick={() => onAdd(quantity, variantId || undefined)}><FiShoppingBag />{available === 0 || (variants.length > 0 && !variantId) ? 'Нет в наличии' : 'В корзину'}</button></div>
        <button className="favorite-action" type="button" aria-pressed={favorite} onClick={onFavorite}><FiHeart />{favorite ? 'Убрать из избранного' : 'Добавить в избранное'}</button>
        <section className="product-reviews"><h3>Отзывы {ratingSummary && `· ${ratingSummary.averageRating.toFixed(1)} / 5 (${ratingSummary.count})`}</h3>
          {reviews.map((review) => <article key={review.id}><b>{'★'.repeat(review.rating)} · {review.username}</b><p>{review.comment}</p></article>)}
          {!reviews.length && <p>Отзывов пока нет.</p>}
          <form onSubmit={submitReview}><label>Оценка<select value={rating} onChange={(event) => setRating(Number(event.target.value))}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} ★</option>)}</select></label><label>Отзыв<textarea maxLength={2000} value={comment} onChange={(event) => setComment(event.target.value)} /></label><button disabled={sendingReview}>{sendingReview ? 'Отправляем…' : 'Оставить отзыв'}</button><small>Отзыв доступен после подтверждённой покупки и проверки модератором.</small></form>
        </section>
      </div>
    </section>
  </div>;
}
