import { useState } from 'react';
import { FiCheck, FiClock, FiShield, FiUser, FiX } from 'react-icons/fi';
import { api, type Seller } from '@makon/api-client';

type Props = { sellers: Seller[]; onReload: () => Promise<void>; onToast: (message: string) => void };

export function ModerationPanel({ sellers, onReload, onToast }: Props) {
  const [busy, setBusy] = useState<string | null>(null);
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [reason, setReason] = useState('');

  async function moderate(id: string, action: 'approve' | 'reject') {
    if (action === 'reject' && !reason.trim()) return;
    setBusy(id);
    try {
      await api.moderateSeller(id, action, action === 'reject' ? reason.trim() : undefined);
      setRejectId(null); setReason('');
      await onReload();
      onToast(action === 'approve' ? 'Продавец одобрен' : 'Заявка отклонена');
    } catch (error) { onToast(error instanceof Error ? error.message : 'Не удалось обработать заявку'); }
    finally { setBusy(null); }
  }

  return <div className="dashboard-panel"><section className="moderation-hero"><div><p>Очередь заявок</p><h2>Модерация продавцов</h2><span>Проверяйте заявку перед одобрением или укажите причину отклонения.</span></div><FiShield /></section>
    <section className="seller-queue"><header><FiClock /><div><h3>Ожидают решения</h3><span>{sellers.length} заявок</span></div></header>
      {sellers.map((seller) => <article key={seller.id}><div className="seller-avatar"><FiUser /></div><div><b>Заявка {seller.id.slice(0, 8)}</b><span>Пользователь: {seller.userId.slice(0, 8)}</span></div><em>{seller.status}</em><button className="approve" disabled={busy === seller.id} onClick={() => moderate(seller.id, 'approve')}><FiCheck />Одобрить</button><button className="reject" disabled={busy === seller.id} onClick={() => { setRejectId(seller.id); setReason(''); }}><FiX />Отклонить</button>
        {rejectId === seller.id && <div className="seller-rejection"><label>Причина отклонения<textarea required maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} /></label><button disabled={!reason.trim() || busy === seller.id} onClick={() => moderate(seller.id, 'reject')}>Подтвердить</button><button onClick={() => setRejectId(null)}>Назад</button></div>}
      </article>)}
      {!sellers.length && <div className="empty-workspace"><FiCheck /><h2>Очередь пуста</h2><p>Новых заявок на модерацию нет.</p></div>}
    </section>
  </div>;
}
