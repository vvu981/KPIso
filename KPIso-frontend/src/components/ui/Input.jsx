/**
 * Input — Componente de campo de formulario profesional
 *
 * Principio S: responsabilidad única de renderizar label + input + validación
 * Principio O: extensible con estilos, estados y validaciones sin modificar la base
 * Principio I: props segregadas — el consumidor provee solo lo que necesita
 * Principio D: depende de abstracciones (estilos CSS) no valores hardcodeados
 *
 * @param {string} label — texto del label (opcional)
 * @param {string} id — id único para accesibilidad
 * @param {string} type — tipo de input ('text', 'email', 'password', 'number', 'tel', etc.)
 * @param {*} value — valor actual
 * @param {(e: Event) => void} onChange — callback de cambio
 * @param {string} placeholder — texto de placeholder
 * @param {boolean} required — si es requerido
 * @param {boolean} disabled — si está deshabilitado
 * @param {string} error — mensaje de error (vacío/undefined si válido)
 * @param {string} success — indicador de éxito (para validación positiva)
 * @param {string} hint — texto de ayuda bajo el input
 * @param {boolean} mono — fuente monoespaciada (para códigos)
 * @param {boolean} autoFocus — enfoque automático
 */
import { IconXCircle, IconCheckCircle } from './Icons.jsx';

export function Input({
    label,
    id,
    type = 'text',
    value,
    onChange,
    placeholder,
    required = false,
    disabled = false,
    error,
    success = false,
    hint,
    mono = false,
    autoFocus = false,
    className = '',
    ...rest
}) {
    const inputClasses = [
        'form-input',
        error ? 'input-error' : success ? 'input-success' : '',
        disabled ? 'input-disabled' : '',
        mono ? 'input-mono' : '',
        className,
    ].filter(Boolean).join(' ');

    return (
        <div className="form-group">
            {label && (
                <label htmlFor={id} className={required ? 'form-label form-label-required' : 'form-label'}>
                    {label}
                </label>
            )}
            <input
                id={id}
                type={type}
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                required={required}
                disabled={disabled}
                autoFocus={autoFocus}
                className={inputClasses}
                aria-invalid={!!error}
                aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
                {...rest}
            />
            {error && (
                <span id={`${id}-error`} className="form-error" role="alert" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <IconXCircle /> {error}
                </span>
            )}
            {success && !error && (
                <span style={{ fontSize: 'var(--text-xs)', color: 'var(--success)', fontWeight: 'var(--font-medium)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <IconCheckCircle /> Válido
                </span>
            )}
            {hint && !error && (
                <span id={`${id}-hint`} className="form-hint">
                    {hint}
                </span>
            )}
        </div>
    );
}
