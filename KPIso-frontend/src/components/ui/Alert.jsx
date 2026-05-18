/**
 * Alert — Mensaje de estado contextual profesional
 *
 * Principio S: responsabilidad única de comunicar feedback al usuario
 * Principio O: extensible con nuevos tipos sin modificar la base
 * Principio L: consumidores pueden usar cualquier tipo intercambiablemente
 * Principio I: props mínimas y bien segregadas
 * Principio D: depende de abstracciones CSS (alert-{type})
 *
 * @param {'error'|'success'|'warning'|'info'} type
 * @param {string} message — contenido del mensaje
 * @param {string} title — título opcional
 * @param {() => void} onClose — callback para cerrar el alert
 */

import { IconXCircle, IconCheckCircle, IconExclamationTriangle, IconInfoCircle, IconXMark } from './Icons.jsx';
import { useState } from 'react';

const TYPE_CONFIG = {
    error: { icon: <IconXCircle />, ariaLabel: 'Error' },
    success: { icon: <IconCheckCircle />, ariaLabel: 'Éxito' },
    warning: { icon: <IconExclamationTriangle />, ariaLabel: 'Advertencia' },
    info: { icon: <IconInfoCircle />, ariaLabel: 'Información' },
};

export function Alert({
    type = 'error',
    message,
    title,
    onClose,
    dismissible = false,
}) {
    const [visible, setVisible] = useState(true);
    if (!message || !visible) return null;

    const config = TYPE_CONFIG[type] || TYPE_CONFIG.error;

    return (
        <div
            className={`alert alert-${type} anim-slide-down`}
            role="alert"
            aria-live="polite"
            aria-label={config.ariaLabel}
        >
            <span className="alert-icon" aria-hidden="true">
                {config.icon}
            </span>
            <div className="alert-content">
                {title && <div className="alert-title">{title}</div>}
                <div className="alert-description">{message}</div>
            </div>
            {dismissible && (
                <button
                    className="alert-close"
                    onClick={() => {
                        setVisible(false);
                        if (onClose) onClose();
                    }}
                    aria-label="Cerrar alerta"
                    type="button"
                >
                    <IconXMark />
                </button>
            )}
        </div>
    );
}
