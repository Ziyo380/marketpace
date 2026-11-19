import { FormEvent, useEffect, useState } from 'react';
import { FiArchive, FiBox, FiCamera, FiChevronDown, FiEdit3, FiImage, FiPackage, FiPlus, FiRefreshCw, FiTrash2, FiUploadCloud } from 'react-icons/fi';
import { api, formatPrice, type Category, type Inventory, type Product, type ProductImage, type ProductVariant, type InventoryMovement } from '@makon/api-client';

type Props = { products: Product[]; categories: Category[]; onReload: () => Promise<void>; onToast: (message: string) => void };
const emptyForm = { categoryId: '', name: '', slug: '', description: '', price: '' };

export function ProductsPanel({ products, categories, onReload, onToast }: Props) {
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try {
      const body = { ...form, price: Number(form.price) };
      if (editing) await api.updateProduct(editing, body); else await api.createProduct(body);
      setForm(emptyForm); setEditing(null); setFormOpen(false); await onReload(); onToast(editing ? 'Товар обновлён' : 'Товар создан как DRAFT');
    } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось сохранить товар'); }
    finally { setBusy(false); }
  }
  function edit(product: Product) { setFormOpen(true); setEditing(product.id); setForm({ categoryId: product.categoryId, name: product.name, slug: product.slug, description: product.description || '', price: String(product.price) }); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  return <div className="dashboard-panel products-layout"><section className="form-card product-form">{formOpen ? <><header><div><p>Product editor</p><h2>{editing ? 'Редактирование товара' : 'Новый товар'}</h2></div><FiBox /></header><form onSubmit={save}><label>Категория<select required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Выберите</option>{categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}</select></label><label>Название<input required maxLength={200} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label>Slug<input required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} /></label><label>Цена, UZS<input required type="number" min="0.01" step="0.01" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></label><label className="full">Описание<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><div className="form-actions full"><button className="primary" disabled={busy}>{busy ? 'Сохраняем…' : editing ? 'Обновить товар' : 'Создать товар'}</button>{formOpen && <button className="secondary" type="button" onClick={() => { setEditing(null); setForm(emptyForm); setFormOpen(false); }}>Отмена</button>}</div></form></> : <div><h2>Товары магазина</h2><button className="primary" onClick={() => setFormOpen(true)}>Создать товар</button></div>}</section><section className="product-list"><header><div><p>Каталог магазина</p><h2>{products.length} товаров</h2></div><button onClick={onReload}><FiRefreshCw />Обновить</button></header>{products.map((product) => <ProductRow key={product.id} product={product} onEdit={() => edit(product)} onReload={onReload} onToast={onToast} />)}{!products.length && <div className="empty-workspace"><FiBox /><h2>Товаров нет</h2><p>Создайте первый товар через форму.</p></div>}</section></div>;
}

function ProductRow({ product, onEdit, onReload, onToast }: { product: Product; onEdit: () => void; onReload: () => Promise<void>; onToast: (message: string) => void }) {
  const [open, setOpen] = useState(false);
  const [inventory, setInventory] = useState<Inventory | null>(null);
  const [images, setImages] = useState<ProductImage[]>([]);
  const [movements, setMovements] = useState<InventoryMovement[]>([]);
  const [toolsError, setToolsError] = useState('');
  const [amount, setAmount] = useState(1);
  const [busy, setBusy] = useState(false);
  async function loadTools() {
    setToolsError('');
    const [stock, media, history] = await Promise.allSettled([api.inventory(product.id), api.myProductImages(product.id), api.inventoryMovements(product.id)]);
    if (stock.status === 'fulfilled') setInventory(stock.value);
    if (media.status === 'fulfilled') setImages([...media.value].sort((a, b) => a.sortOrder - b.sortOrder));
    if (history.status === 'fulfilled') setMovements(history.value);
    const failures = [stock, media, history].filter((r) => r.status === 'rejected');
    if (failures.length) setToolsError('Часть данных не загрузилась. Обновите инструменты товара.');
  }
  useEffect(() => { if (open) void loadTools(); }, [open]);
  async function action(name: 'publish' | 'archive' | 'restore') { setBusy(true); try { await api.productAction(product.id, name); await onReload(); onToast(`Статус товара обновлён: ${name}`); } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить статус'); } finally { setBusy(false); } }
  async function stock(kind: 'add' | 'remove') { setBusy(true); try { setInventory(kind === 'add' ? await api.addStock(product.id, amount) : await api.removeStock(product.id, amount)); await loadTools(); onToast('Остаток обновлён'); } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить остаток'); } finally { setBusy(false); } }
  async function upload(file?: File) { if (!file) return; setBusy(true); try { await api.uploadProductImage(product.id, file); await loadTools(); onToast('Изображение загружено'); } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось загрузить изображение'); } finally { setBusy(false); } }
  async function primary(imageId: string) { setBusy(true); try { await api.setPrimaryProductImage(product.id, imageId); await loadTools(); onToast('Главное изображение изменено'); } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось изменить изображение'); } finally { setBusy(false); } }
  async function removeImage(imageId: string) { setBusy(true); try { await api.deleteProductImage(product.id, imageId); await loadTools(); onToast('Изображение удалено'); } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось удалить изображение'); } finally { setBusy(false); } }
  async function moveImage(index: number, direction: number) {
    const next = [...images]; const target = index + direction;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    setBusy(true);
    try { setImages(await api.reorderImages(product.id, next.map((image) => image.id))); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось изменить порядок фото'); }
    finally { setBusy(false); }
  }
  return <article className="dashboard-product"><button className="dashboard-product__summary" onClick={() => setOpen((value) => !value)}><div className="product-avatar"><FiPackage /></div><span><b>{product.name}</b><small>{product.slug} · {formatPrice(product.price)}</small></span><em data-status={product.status}>{product.status}</em><FiChevronDown className={open ? 'rotate' : ''} /></button>{open && <div className="dashboard-product__tools">{toolsError && <p role="alert">{toolsError}</p>}<button disabled={busy} onClick={loadTools}>Обновить инструменты</button><div className="product-tool-actions"><button onClick={onEdit}><FiEdit3 />Редактировать</button>{product.status === 'DRAFT' && <button onClick={() => action('publish')} disabled={busy}><FiUploadCloud />Опубликовать</button>}{product.status === 'PUBLISHED' && <button onClick={() => action('archive')} disabled={busy}><FiArchive />В архив</button>}{product.status === 'ARCHIVED' && <button onClick={() => action('restore')} disabled={busy}><FiRefreshCw />В черновик</button>}</div><section className="stock-card"><div><small>Количество</small><b>{inventory?.quantity ?? '—'}</b></div><div><small>Резерв</small><b>{inventory?.reservedQuantity ?? '—'}</b></div><div><small>Доступно</small><b>{inventory?.availableQuantity ?? '—'}</b></div><label>Изменение<input type="number" min="1" value={amount} onChange={(event) => setAmount(Math.max(1, Number(event.target.value)))} /></label><button disabled={busy || !Number.isInteger(amount) || amount < 1} onClick={() => stock('add')}><FiPlus />Добавить</button><button disabled={busy || !Number.isInteger(amount) || amount < 1} onClick={() => stock('remove')}>Списать</button></section><details className="stock-history"><summary>История складских операций</summary>{movements.map((movement) => <p key={movement.id}>{new Date(movement.createdAt).toLocaleString('ru-RU')} · {movement.type} · Остаток: {movement.quantityDelta > 0 ? '+' : ''}{movement.quantityDelta}, резерв: {movement.reservedDelta > 0 ? '+' : ''}{movement.reservedDelta} · После: {movement.quantityAfter} / резерв {movement.reservedAfter}</p>)}{!movements.length && <p>Операций пока нет</p>}</details><VariantTools productId={product.id} onToast={onToast} /><section className="media-card"><header><div><FiCamera /><span><b>Изображения</b><small>{images.length} файлов</small></span></div><label><FiUploadCloud />Загрузить<input type="file" accept="image/*" disabled={busy} hidden onChange={(event) => upload(event.target.files?.[0])} /></label></header><div>{images.map((image, index) => <article key={image.id}><img src={image.url} alt={image.originalFilename} /><button disabled={busy || index === 0} onClick={() => moveImage(index, -1)} aria-label="Фото раньше">←</button><button disabled={busy || index === images.length - 1} onClick={() => moveImage(index, 1)} aria-label="Фото позже">→</button><span>{image.primary ? 'Главное' : `#${image.sortOrder}`}</span><button disabled={busy} onClick={() => primary(image.id)} aria-label="Сделать главным"><FiImage /></button><button disabled={busy} onClick={() => removeImage(image.id)} aria-label="Удалить"><FiTrash2 /></button></article>)}{!images.length && <p>Изображений пока нет. Загрузите первую фотографию товара.</p>}</div></section></div>}</article>;
}

function VariantTools({ productId, onToast }: { productId: string; onToast: (message: string) => void }) {
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [form, setForm] = useState({ name: '', sku: '', price: '' });
  const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(null);
  const [busy, setBusy] = useState(false);
  async function reload() { setVariants(await api.myVariants(productId)); }
  useEffect(() => { void reload().catch((error) => onToast(error instanceof Error ? error.message : 'Не удалось загрузить варианты')); }, [productId]);
  async function create(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try { await api.createVariant(productId, { name: form.name, sku: form.sku, price: Number(form.price) }); setForm({ name: '', sku: '', price: '' }); await reload(); onToast('Вариант создан'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось создать вариант'); }
    finally { setBusy(false); }
  }
  async function stock(variant: ProductVariant, kind: 'add' | 'remove') {
    const raw = window.prompt('Количество единиц', '1');
    if (raw === null) return;
    const amount = Number(raw);
    if (!Number.isInteger(amount) || amount < 1) { onToast('Введите целое положительное количество'); return; }
    setBusy(true);
    try { await api.changeVariantStock(productId, variant.id, kind, amount); await reload(); onToast('Остаток варианта обновлён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить остаток'); }
    finally { setBusy(false); }
  }
  async function toggle(variant: ProductVariant) {
    setBusy(true);
    try { await api.updateVariant(productId, variant.id, { name: variant.name, price: variant.price, active: !variant.active }); await reload(); onToast('Вариант обновлён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обновить вариант'); }
    finally { setBusy(false); }
  }
  async function saveVariant(event: FormEvent) {
    event.preventDefault(); if (!editingVariant) return; setBusy(true);
    try { await api.updateVariant(productId, editingVariant.id, { name: editingVariant.name.trim(), price: editingVariant.price, active: editingVariant.active }); setEditingVariant(null); await reload(); onToast('Вариант сохранён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось сохранить вариант'); }
    finally { setBusy(false); }
  }
  return <section className="variant-card" aria-label="Варианты товара"><h3>Варианты · {variants.length}</h3>
    {variants.map((variant) => <div key={variant.id}><b>{variant.name}</b><small>{variant.sku} · {formatPrice(variant.price)} · доступно {variant.availableQuantity}</small><button disabled={busy} onClick={() => stock(variant, 'add')}>+ Остаток</button><button disabled={busy} onClick={() => stock(variant, 'remove')}>− Остаток</button><button disabled={busy} onClick={() => setEditingVariant({ ...variant })}>Редактировать</button><button disabled={busy} onClick={() => toggle(variant)}>{variant.active ? 'Отключить' : 'Включить'}</button></div>)}
    {editingVariant && <form onSubmit={saveVariant}><label>Название<input required maxLength={120} value={editingVariant.name} onChange={(e) => setEditingVariant({ ...editingVariant, name: e.target.value })} /></label><label>Цена<input required type="number" min="0.01" step="0.01" value={editingVariant.price} onChange={(e) => setEditingVariant({ ...editingVariant, price: Number(e.target.value) })} /></label><button disabled={busy}>Сохранить вариант</button><button type="button" onClick={() => setEditingVariant(null)}>Отмена</button></form>}
    <form onSubmit={create}><label>Вариант<input required maxLength={120} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Цвет / размер" /></label><label>SKU<input required pattern="[A-Za-z0-9_-]{3,80}" value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} /></label><label>Цена, UZS<input required type="number" min="0.01" step="0.01" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></label><button disabled={busy}>Создать вариант</button></form>
  </section>;
}
