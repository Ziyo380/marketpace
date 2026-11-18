'use client';

import type { CSSProperties } from 'react';
import { FiArrowUpRight, FiHeart, FiShoppingBag } from 'react-icons/fi';
import type { Product } from '@makon/api-client';
import { formatPrice } from '@makon/api-client';
import { fallbackProductImage } from './product-images';

type Props = {
  product: Product;
  index: number;
  favorite: boolean;
  onFavorite: () => void;
  onOpen: () => void;
  onAdd: () => void;
};

export function ProductCard({ product, index, favorite, onFavorite, onOpen, onAdd }: Props) {
  const image = product.primaryImageUrl || fallbackProductImage(product);
  return (
    <article className="market-product" style={{ '--card-order': index } as CSSProperties}>
      <div className="market-product__media">
        <img src={image} alt={product.name} loading="lazy" onError={(event) => { event.currentTarget.src = fallbackProductImage(product); }} />
        <span className="market-product__status">В каталоге</span>
        <button className="market-product__favorite" type="button" aria-label={favorite ? 'Убрать из избранного' : 'Добавить в избранное'} aria-pressed={favorite} onClick={onFavorite}>
          <FiHeart aria-hidden="true" />
        </button>
      </div>
      <div className="market-product__body">
        <p>{product.storeSlug}</p>
        <h3>{product.name}</h3>
        <span>{product.description || 'Описание товара скоро появится.'}</span>
        <div className="market-product__footer">
          <strong>{formatPrice(product.price)}</strong>
          <div>
            <button type="button" onClick={onAdd} aria-label={`Добавить ${product.name} в корзину`}><FiShoppingBag aria-hidden="true" /></button>
            <button type="button" onClick={onOpen} aria-label={`Подробнее о ${product.name}`}><FiArrowUpRight aria-hidden="true" /></button>
          </div>
        </div>
      </div>
    </article>
  );
}
