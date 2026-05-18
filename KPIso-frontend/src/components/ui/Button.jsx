/**
 * Button — Componente atómico de botón profesional
 *
 * Principio S: responsabilidad única de renderizar un botón accesible
 * Principio L: cualquier consumidor puede sustituir un botón por otro cambiando props
 * Principio I: props segregadas — el consumidor solo provee lo que necesita
 * Principio D: depende de abstracciones (variant, size, state) no valores concretos
 *
 * @param {'primary'|'secondary'|'danger'|'success'|'warning'|'ghost'|'outline'|'cyan'} variant
 * @param {'xs'|'sm'|'md'|'lg'|'xl'} size
 * @param {boolean} loading — muestra spinner y bloquea el botón
 * @param {boolean} full — 100% de ancho
 * @param {boolean} disabled — estado deshabilitado
 * @param {'button'|'submit'|'reset'} type
 * @param {() => void} onClick
 */
export function Button({
    children,
    variant = 'primary',
    size = 'md',
    loading = false,
    full = false,
    type = 'button',
    disabled = false,
    onClick,
    className = '',
    title,
    ...rest
}) {
    const classes = [
        'btn',
        `btn-${variant}`,
        `btn-${size}`,
        full ? 'btn-full' : '',
        loading ? 'btn-loading' : '',
        className,
    ].filter(Boolean).join(' ');

    return (
        <button
            type={type}
            className={classes}
            disabled={disabled || loading}
            onClick={onClick}
            title={title}
            aria-busy={loading}
            {...rest}
        >
            {!loading && children}
        </button>
    );
}
