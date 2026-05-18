/**
 * Modal — Diálogo flotante genérico con backdrop blur
 *
 * Principio S: responsabilidad única de gestionar el portal de capas modales.
 * Principio O: extensible con tamaños y contenido arbitrario sin modificar la estructura.
 *
 * @param {boolean} show
 * @param {() => void} onClose
 * @param {string} title
 * @param {'sm'|'md'|'lg'} size
 */
import { useEffect } from 'react';
import { IconXMark } from './Icons.jsx';

const SIZE_MAP = {
    sm: '380px',
    md: '480px',
    lg: '620px',
};

export function Modal({ show, onClose, title, children, size = 'md' }) {
    // Cierra con Escape
    useEffect(() => {
        if (!show) return;
        const handler = (e) => { if (e.key === 'Escape') onClose?.(); };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [show, onClose]);

    // Bloquea scroll del body
    useEffect(() => {
        document.body.style.overflow = show ? 'hidden' : '';
        return () => { document.body.style.overflow = ''; };
    }, [show]);

    if (!show) return null;

    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                background: 'var(--bg-overlay)',
                backdropFilter: 'blur(6px)',
                WebkitBackdropFilter: 'blur(6px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 'var(--z-modal)',
                padding: 'var(--space-4)',
            }}
            onClick={(e) => { if (e.target === e.currentTarget) onClose?.(); }}
        >
            <div
                className="card anim-scale-in"
                style={{
                    width: '100%',
                    maxWidth: SIZE_MAP[size] ?? SIZE_MAP.md,
                    maxHeight: '90vh',
                    overflowY: 'auto',
                    padding: 'var(--space-6)',
                    background: 'var(--bg-surface)',
                    boxShadow: 'var(--shadow-xl)',
                }}
            >
                {/* Header del modal */}
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: 'var(--space-5)',
                }}>
                    <h2 style={{
                        fontSize: 'var(--text-base)',
                        fontWeight: 700,
                        color: 'var(--text-primary)',
                        letterSpacing: '-0.01em',
                    }}>
                        {title}
                    </h2>
                    {onClose && (
                        <button
                            onClick={onClose}
                            className="btn btn-ghost btn-icon-only btn-sm"
                            aria-label="Cerrar"
                            style={{ fontSize: '1.1rem', color: 'var(--text-secondary)' }}
                        >
                            <IconXMark />
                        </button>
                    )}
                </div>

                {/* Contenido */}
                {children}
            </div>
        </div>
    );
}
