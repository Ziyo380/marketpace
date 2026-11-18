'use client';

import { FormEvent, useState } from 'react';
import { FiArrowRight, FiShield, FiX } from 'react-icons/fi';
import { api, authStore, type UserProfile } from '@makon/api-client';

type Props = { initialMode?: 'login' | 'register'; onClose: () => void; onAuthenticated: (profile: UserProfile) => void; onToast: (message: string) => void };

export function AuthDialog({ initialMode = 'login', onClose, onAuthenticated, onToast }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      if (mode === 'register') {
        await api.register({ username, password });
        setMode('login'); onToast('Аккаунт создан — теперь войдите');
        return;
      }
      const session = await api.login({ username, password });
      authStore.setTokens(session.accessToken, session.refreshToken);
      onAuthenticated(await api.profile());
      onToast('Вы вошли в Makon');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Не удалось выполнить запрос');
    } finally { setBusy(false); }
  }

  return <div className="overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
      <button className="icon-button dialog-close" type="button" onClick={onClose} aria-label="Закрыть"><FiX /></button>
      <div className="auth-dialog__mark"><FiShield /></div>
      <p className="eyebrow">Безопасный доступ</p>
      <h2 id="auth-title">{mode === 'login' ? 'С возвращением' : 'Создайте аккаунт'}</h2>
      <p>Войдите, чтобы сохранить корзину и следить за заказами.</p>
      <div className="auth-tabs"><button type="button" aria-pressed={mode === 'login'} onClick={() => setMode('login')}>Вход</button><button type="button" aria-pressed={mode === 'register'} onClick={() => setMode('register')}>Регистрация</button></div>
      <form onSubmit={submit}>
        <label>Логин<input required minLength={3} value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" /></label>
        <label>Пароль<input required minLength={8} type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} /></label>
        {error && <div className="form-error">{error}</div>}
        <button className="action-primary action-wide" disabled={busy}>{busy ? 'Подождите…' : mode === 'login' ? 'Войти' : 'Создать аккаунт'}<FiArrowRight /></button>
      </form>
      <div className="demo-accounts"><span>Демо-доступ</span><button type="button" onClick={() => { setUsername('demo-customer'); setPassword('Customer123!'); }}>Покупатель</button><button type="button" onClick={() => { setUsername('demo-seller'); setPassword('Seller123!'); }}>Продавец</button></div>
    </section>
  </div>;
}
