import { useState, type FormEvent } from 'react';
import { FiEdit3, FiFolder, FiPlus, FiSlash } from 'react-icons/fi';
import { api, type Category } from '@makon/api-client';

type Form = { name: string; slug: string; description: string; parentId: string; active: boolean };
const emptyForm: Form = { name: '', slug: '', description: '', parentId: '', active: true };

export function CategoriesPanel({ categories, onReload, onToast }: { categories: Category[]; onReload: () => Promise<void>; onToast: (message: string) => void }) {
  const [form, setForm] = useState<Form>(emptyForm);
  const [editing, setEditing] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function edit(category: Category) {
    setEditing(category.id);
    setForm({ name: category.name, slug: category.slug, description: category.description || '', parentId: category.parentId || '', active: category.isActive });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try {
      const body = { name: form.name.trim(), slug: form.slug.trim(), description: form.description.trim(), parentId: form.parentId || null };
      if (editing) await api.updateCategory(editing, { ...body, active: form.active });
      else await api.createCategory(body);
      onToast(editing ? 'Категория обновлена' : 'Категория создана');
      setEditing(null); setForm(emptyForm); await onReload();
    } catch (caught) { onToast(caught instanceof Error ? caught.message : 'Не удалось сохранить категорию'); }
    finally { setBusy(false); }
  }

  async function deactivate(category: Category) {
    setBusy(true);
    try { await api.deactivateCategory(category.id); await onReload(); onToast('Категория отключена'); }
    catch (caught) { onToast(caught instanceof Error ? caught.message : 'Не удалось отключить категорию'); }
    finally { setBusy(false); }
  }

  return <div className="dashboard-panel categories-layout"><section className="form-card"><header><div><p>Управление каталогом</p><h2>{editing ? 'Изменить категорию' : 'Новая категория'}</h2></div><FiFolder /></header><form onSubmit={save}>
    <label>Название<input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
    <label>Slug<input required maxLength={120} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} /></label>
    <label className="full">Родительская категория<select value={form.parentId} onChange={(event) => setForm({ ...form, parentId: event.target.value })}><option value="">Корневая категория</option>{categories.filter((category) => category.id !== editing && category.isActive).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
    <label className="full">Описание<textarea maxLength={1000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
    {editing && <label className="category-active full"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />Активная категория</label>}
    <div className="form-actions full"><button className="primary" disabled={busy}><FiPlus />{busy ? 'Сохраняем…' : editing ? 'Сохранить' : 'Создать категорию'}</button>{editing && <button type="button" className="secondary" onClick={() => { setEditing(null); setForm(emptyForm); }}>Отмена</button>}</div>
  </form></section><section className="category-admin-list"><header><p>Разделы витрины</p><h2>{categories.length} категорий</h2></header>{categories.map((category) => <article key={category.id}><div><b>{category.name}</b><small>{category.parentId ? 'Вложенная' : 'Корневая'} · {category.slug}</small></div><span data-active={category.isActive}>{category.isActive ? 'Активна' : 'Отключена'}</span><button onClick={() => edit(category)} aria-label={`Изменить ${category.name}`}><FiEdit3 /></button>{category.isActive && <button disabled={busy} onClick={() => deactivate(category)} aria-label={`Отключить ${category.name}`}><FiSlash /></button>}</article>)}</section></div>;
}
