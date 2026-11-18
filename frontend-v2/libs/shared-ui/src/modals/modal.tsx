'use client';

import { useEffect, useId, useRef, type ReactNode } from 'react';

type ModalProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ open, title, onClose, children }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const previousFocus = useRef<HTMLElement | null>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      previousFocus.current = document.activeElement as HTMLElement | null;
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  function handleClose() {
    onClose();
    previousFocus.current?.focus();
  }

  return (
    <dialog ref={ref} className="ds-modal" aria-labelledby={titleId} onClose={handleClose}>
      <button type="button" className="ds-button ds-button--ghost ds-modal__close" aria-label="Закрыть" onClick={() => ref.current?.close()}>×</button>
      <h2 id={titleId} className="font-display text-3xl text-gold">{title}</h2>
      {children}
    </dialog>
  );
}
