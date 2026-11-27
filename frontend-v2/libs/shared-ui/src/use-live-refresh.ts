'use client';

import { useEffect, useRef } from 'react';

export function useLiveRefresh(refresh: () => void | Promise<void>, enabled = true, delay = 10000) {
  const callback = useRef(refresh);
  callback.current = refresh;
  useEffect(() => {
    if (!enabled) return;
    let running = false;
    const update = async () => {
      if (document.visibilityState !== 'visible' || running) return;
      running = true;
      try { await callback.current(); } catch { /* Keep the current view during a temporary connection failure. */ }
      finally { running = false; }
    };
    const interval = window.setInterval(update, delay);
    window.addEventListener('focus', update);
    document.addEventListener('visibilitychange', update);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', update);
      document.removeEventListener('visibilitychange', update);
    };
  }, [enabled, delay]);
}
