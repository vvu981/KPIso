/**
 * Badge — Etiqueta compacta de estado, categoría o información
 *
 * Principio S: responsabilidad única de mostrar información de estado breve
 * Principio O: extensible con variantes sin modificar la base
 * Principio L: consumidores pueden usar cualquier variante de forma intercambiable
 * Principio I: props mínimas y bien definidas
 * Principio D: depende de clases CSS (badge-{variant})
 *
 * @param {'primary'|'cyan'|'success'|'danger'|'warning'|'info'|'neutral'} variant
 * @param {'xs'|'sm'|'lg'} size
 */
export function Badge({ children, variant = 'neutral', size = 'sm', className = '' }) {
    const classes = [
        'badge',
        `badge-${variant}`,
        size !== 'sm' ? `badge-${size}` : '',
        className,
    ].filter(Boolean).join(' ');

    return (
        <span className={classes} role="status">
            {children}
        </span>
    );
}
