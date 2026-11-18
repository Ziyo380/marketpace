'use client';

import { FiMenu, FiShoppingBag, FiUser, FiX } from 'react-icons/fi';
import { useState } from 'react';
import type { UserProfile } from '@makon/api-client';

type Props = { apiOnline: boolean; loading: boolean; profile: UserProfile | null; cartCount: number; onAuth: () => void; onAccount: () => void; onCart: () => void };

export function MarketplaceHeader({ apiOnline, loading, profile, cartCount, onAuth, onAccount, onCart }: Props) {
  const [open, setOpen] = useState(false);
  return <header className="market-header"><div className="market-container market-header__inner">
    <a className="market-logo" href="#top"><span>M</span><div><b>MAKON</b><small>marketplace</small></div></a>
    <nav data-open={open}><a href="#catalog" onClick={() => setOpen(false)}>Каталог</a><a href="#categories" onClick={() => setOpen(false)}>Категории</a><a href="#trust" onClick={() => setOpen(false)}>Как это работает</a><a href="http://localhost:4200" target="_blank" rel="noreferrer">Для продавцов</a></nav>
    <div className="market-header__actions"><span className={apiOnline ? 'api-state online' : 'api-state'}><i />{loading ? 'Загружаем каталог' : apiOnline ? 'Каталог доступен' : 'Каталог недоступен'}</span><button className="header-icon" type="button" onClick={onCart} aria-label="Корзина"><FiShoppingBag />{cartCount > 0 && <b>{cartCount}</b>}</button><button className="profile-button" type="button" onClick={profile ? onAccount : onAuth}><FiUser /><span>{profile?.username || 'Войти'}</span></button><button className="mobile-menu" onClick={() => setOpen((value) => !value)} aria-label="Меню">{open ? <FiX /> : <FiMenu />}</button></div>
  </div></header>;
}
