import { FormEvent, useState } from 'react';
import { FiArrowRight, FiLock, FiShield } from 'react-icons/fi';
import { api, authStore } from '@makon/api-client';

type Props = { onReady: () => Promise<void>; onToast: (message: string) => void; toast: string };

export function DashboardLogin({ onReady, onToast, toast }: Props) {
  const [username, setUsername] = useState('demo-seller');
  const [password, setPassword] = useState('Seller123!');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try { const result = await api.login({ username, password }); authStore.setTokens(result.accessToken, result.refreshToken); await onReady(); onToast('Кабинет подключён'); }
    catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось войти'); }
    finally { setBusy(false); }
  }
  return <main className="dashboard-login"><section><div className="login-brand"><span>M</span><div><b>MAKON</b><small>workspace</small></div></div><div className="login-copy"><p><FiShield />Защищённый кабинет</p><h1>Управляйте<br/><em>своим рынком.</em></h1><span>Товары, остатки, изображения и модерация — в отдельном рабочем пространстве.</span></div></section><form onSubmit={submit}><div className="login-icon"><FiLock /></div><p>Ваш рабочий кабинет</p><h2>Вход в кабинет</h2><label>Логин<input required value={username} onChange={(event) => setUsername(event.target.value)} /></label><label>Пароль<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{toast && <div className="login-message">{toast}</div>}<button disabled={busy}>{busy ? 'Подключаем…' : 'Продолжить'}<FiArrowRight /></button><div className="login-presets"><button type="button" onClick={() => { setUsername('demo-seller'); setPassword('Seller123!'); }}>Продавец</button><button type="button" onClick={() => { setUsername('demo-admin'); setPassword('Admin123!'); }}>Администратор</button></div></form></main>;
}
