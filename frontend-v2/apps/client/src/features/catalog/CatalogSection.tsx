'use client';

import { FiFilter, FiSearch, FiSliders } from 'react-icons/fi';
import type { Category, Product, Store } from '@makon/api-client';
import { ProductCard } from './ProductCard';

export type CatalogFilters = {
  search: string;
  categoryId: string;
  minPrice: string;
  maxPrice: string;
  sort: string;
  storeSlug: string;
  availableOnly: string;
};

type Props = {
  categories: Category[];
  stores: Store[];
  products: Product[];
  loading: boolean;
  loadingMore: boolean;
  totalElements: number;
  hasMore: boolean;
  filters: CatalogFilters;
  favorites: Set<string>;
  onFilters: (next: CatalogFilters) => void;
  onApply: () => void;
  onCategory: (categoryId: string) => void;
  onMore: () => void;
  onReset: () => void;
  onOpen: (product: Product) => void;
  onFavorite: (product: Product) => void;
  onAdd: (product: Product) => void;
};

export function CatalogSection({ categories, stores, products, loading, loadingMore, totalElements, hasMore, filters, favorites, onFilters, onApply, onCategory, onMore, onReset, onOpen, onFavorite, onAdd }: Props) {
  const update = (key: keyof CatalogFilters, value: string) => onFilters({ ...filters, [key]: value });
  return (
    <section className="catalog-shell" id="catalog">
      <header className="section-title">
        <div><span>Живая витрина</span><h2>Товары, которые выбирают сейчас</h2></div>
        <p>{loading ? 'Получаем каталог…' : `${totalElements} товаров найдено`}</p>
      </header>

      <div className="category-tabs" role="tablist" aria-label="Категории товаров">
        <button type="button" role="tab" aria-selected={filters.categoryId === 'all'} onClick={() => onCategory('all')}>Все товары</button>
        {categories.filter((category) => category.isActive).map((category) => (
          <button key={category.id} type="button" role="tab" aria-selected={filters.categoryId === category.id} onClick={() => onCategory(category.id)}>{category.name}</button>
        ))}
      </div>

      <div className="catalog-tools">
        <label className="catalog-search"><FiSearch aria-hidden="true" /><input value={filters.search} onChange={(event) => update('search', event.target.value)} onKeyDown={(event) => event.key === 'Enter' && onApply()} placeholder="Название товара" /></label>
        <label><span>Цена от</span><input type="number" min="0" value={filters.minPrice} onChange={(event) => update('minPrice', event.target.value)} placeholder="0" /></label>
        <label><span>Цена до</span><input type="number" min="0" value={filters.maxPrice} onChange={(event) => update('maxPrice', event.target.value)} placeholder="Без лимита" /></label>
        <label><span>Магазин</span><select value={filters.storeSlug} onChange={(event) => update('storeSlug', event.target.value)}><option value="all">Все магазины</option>{stores.map((store) => <option key={store.id} value={store.slug}>{store.name}</option>)}</select></label>
        <label><span>Наличие</span><select value={filters.availableOnly} onChange={(event) => update('availableOnly', event.target.value)}><option value="false">Все товары</option><option value="true">В наличии</option></select></label>
        <label><span>Сортировка</span><select value={filters.sort} onChange={(event) => update('sort', event.target.value)}>
          <option value="createdAt,desc">Сначала новые</option><option value="price,asc">Сначала дешевле</option><option value="price,desc">Сначала дороже</option><option value="name,asc">По названию</option>
        </select></label>
        <button className="action-primary" type="button" onClick={onApply}><FiFilter aria-hidden="true" />Применить</button>
        <button className="action-ghost" type="button" onClick={onReset}><FiSliders aria-hidden="true" />Сбросить</button>
      </div>

      <div className="market-grid">
        {loading && Array.from({ length: 6 }, (_, index) => <div className="market-product market-product--skeleton" key={index}><div/><span/><span/><span/></div>)}
        {!loading && products.map((product, index) => <ProductCard key={product.id} product={product} index={index} favorite={favorites.has(product.id)} onFavorite={() => onFavorite(product)} onOpen={() => onOpen(product)} onAdd={() => onAdd(product)} />)}
        {!loading && products.length === 0 && <div className="catalog-empty"><FiSearch aria-hidden="true" /><h3>Ничего не найдено</h3><p>Измените фильтры или сбросьте поиск.</p><button className="action-primary" onClick={onReset}>Показать всё</button></div>}
      </div>
      {hasMore && <div className="catalog-more"><button type="button" className="action-ghost" disabled={loadingMore} onClick={onMore}>{loadingMore ? 'Загружаем…' : `Показать ещё · ${products.length} из ${totalElements}`}</button></div>}
    </section>
  );
}
