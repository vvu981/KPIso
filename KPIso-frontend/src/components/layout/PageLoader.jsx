/**
 * PageLoader — Indicador de carga de página centralizado
 *
 * Principio S: responsabilidad única de mostrar estado de carga
 * Principio O: extensible con variantes y mensajes sin modificar la base
 * Principio I: props mínimas y bien segregadas
 * Principio D: depende de clases CSS (page-loader, loader-ring, etc.)
 *
 * @param {string} message — texto opcional bajo el spinner
 * @param {'sm'|'md'|'lg'} size — tamaño del spinner
 * @param {'primary'|'cyan'|'gradient'} variant — variante de color
 */
export function PageLoader({
    message = 'Cargando...',
    size = 'md',
    variant = 'primary',
}) {
    const sizeMap = {
        sm: 40,
        md: 60,
        lg: 80,
    };

    const spinnerSize = sizeMap[size] || sizeMap.md;
    const logoSize = spinnerSize * 0.35;

    return (
        <div
            className="page-loader"
            role="status"
            aria-busy="true"
            aria-label={message}
        >
            {/* Spinner Container */}
            <div
                style={{
                    position: 'relative',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: spinnerSize,
                    height: spinnerSize,
                }}
            >
                {/* Anillo de carga */}
                <div
                    className={`loader-ring loader-ring-${variant}`}
                    style={{
                        width: '100%',
                        height: '100%',
                    }}
                    aria-hidden="true"
                />

                {/* Logo centrado */}
                <div
                    style={{
                        position: 'absolute',
                        width: logoSize,
                        height: logoSize,
                        borderRadius: 'var(--radius-sm)',
                        background: variant === 'gradient' ? 'var(--gradient-brand)' : `var(--${variant}-light)`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: `${logoSize * 0.45}px`,
                        fontWeight: 'var(--font-black)',
                        color: '#fff',
                        letterSpacing: '-0.03em',
                        boxShadow: 'var(--shadow-md)',
                    }}
                    aria-hidden="true"
                >
                    K
                </div>
            </div>

            {/* Mensaje de carga */}
            {message && (
                <p className="loader-text">
                    {message}
                    <span style={{ animation: 'anim-pulse-glow 2s infinite' }} aria-hidden="true">
                        •••
                    </span>
                </p>
            )}
        </div>
    );
}
