import { useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

export default function PetFormModal({ children, title }: { children: ReactNode; title: string }) {
  const navigate = useNavigate();
  const dialogRef = useRef<HTMLElement>(null);
  const backRef = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    backRef.current?.focus();
    return () => { document.body.style.overflow = overflow; };
  }, []);

  function trapFocus(event: KeyboardEvent<HTMLElement>) {
    if (event.key !== 'Tab') return;
    const elements = dialogRef.current?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), textarea:not(:disabled), a[href], [tabindex]:not([tabindex="-1"])');
    if (!elements?.length) return;
    const first = elements[0];
    const last = elements[elements.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }

  return <main ref={dialogRef} className="modal-page" role="dialog" aria-modal="true" aria-labelledby="pet-form-title" aria-label={title} onKeyDown={trapFocus}>
    <button ref={backRef} className="button button-primary modal-back-button" type="button" onClick={() => navigate('/pets')}>
      <span aria-hidden="true">←</span>
    </button>
    {children}
  </main>;
}
