/**
 * Card — Componente contenedor visual profesional
 *
 * Principio S: responsabilidad única de proveer una superficie de contenido estilizada
 * Principio O: extensible con variantes (glass, interactive) sin modificar la base
 * Principio L: los consumidores pueden sustituir cards de forma intercambiable
 * Principio I: props bien segregadas y opcionales
 * Principio D: depende de abstracciones CSS (glass-bg, card-interactive, etc.)
 *
 * @param {'none'|'sm'|'md'|'lg'} padding — padding interior
 * @param {boolean} glass — glassmorphism effect
 * @param {boolean} interactive — hover elevation + cursor pointer
 * @param {boolean} elevated — sombra md en lugar de sm
 * @param {() => void} onClick — callback si es interactiva
 */
export function Card({
    children,
    glass = false,
    interactive = false,
    elevated = false,
    padding = 'md',
    className = '',
    onClick,
    style,
}) {
    const paddingMap = {
        'none': '',
        'sm': 'card-body-sm',
        'md': 'card-body',
        'lg': 'card-body-lg',
    };

    const classes = [
        glass ? 'card-glass' : 'card',
        interactive ? 'card-interactive' : '',
        elevated ? 'card-elevated' : '',
        paddingMap[padding] ?? 'card-body',
        className,
    ].filter(Boolean).join(' ');

    return (
        <div
            className={classes}
            onClick={onClick}
            style={style}
            role={interactive ? 'button' : 'region'}
            tabIndex={interactive ? 0 : undefined}
            onKeyDown={interactive ? (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    onClick?.();
                }
            } : undefined}
        >
            {children}
        </div>
    );
}
