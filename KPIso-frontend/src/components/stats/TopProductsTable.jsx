/**
 * TopProductsTable — Tabla reutilizable para listas de productos.
 *
 * Props:
 *   title:   string   — Encabezado de la tabla.
 *   rows:    string[] — Nombres de productos (el backend devuelve solo el nombre, LIMIT 5).
 *   valueLabel: string — Etiqueta de la segunda columna (ej: "Recuento" o "Precio").
 *   values:  (string|number)[] — Valores correspondientes (opcional; si no se pasa, solo se muestra el nombre).
 */
export default function TopProductsTable({ title, rows = [] }) {
    return (
        <div
            style={{
                borderRadius: 'var(--radius-lg)',
                background: 'var(--bg-glass)',
                backdropFilter: 'blur(12px)',
                border: '1px solid var(--border-subtle)',
                overflow: 'hidden',
            }}
        >
            <h3
                style={{
                    padding: 'var(--space-4)',
                    margin: 0,
                    fontSize: 'var(--text-base)',
                    fontWeight: 'var(--font-bold)',
                    color: 'var(--text-primary)',
                    borderBottom: '1px solid var(--border-subtle)',
                }}
            >
                {title}
            </h3>

            {rows.length === 0 ? (
                <p style={{ padding: 'var(--space-4)', color: 'var(--text-secondary)', textAlign: 'center' }}>
                    Sin datos para el período seleccionado.
                </p>
            ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 'var(--text-sm)' }}>
                    <thead>
                        <tr style={{ background: 'var(--bg-soft)' }}>
                            <th style={thStyle}>#</th>
                            <th style={thStyle}>Producto</th>
                            <th style={{ ...thStyle, textAlign: 'right' }}>Precio/Ud</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((item, idx) => (
                            <tr
                                key={idx}
                                style={{ borderBottom: '1px solid var(--border-subtle)', transition: 'background 150ms' }}
                                onMouseEnter={(ev) => (ev.currentTarget.style.background = 'var(--bg-soft)')}
                                onMouseLeave={(ev) => (ev.currentTarget.style.background = 'transparent')}
                            >
                                <td style={{ ...tdStyle, color: 'var(--text-secondary)', width: 32 }}>{idx + 1}</td>
                                <td style={tdStyle}>{item.name || item}</td>
                                <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 'var(--font-bold)' }}>
                                    {item.unitPrice !== undefined ? `${item.unitPrice.toFixed(2)}€` : '—'}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}

// ── Estilos de tabla reutilizables ─────────────────────────────────
const thStyle = {
    padding: 'var(--space-2) var(--space-3)',
    textAlign: 'left',
    fontWeight: 'var(--font-semibold)',
    color: 'var(--text-secondary)',
    fontSize: 'var(--text-xs)',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
};

const tdStyle = {
    padding: 'var(--space-2) var(--space-3)',
    color: 'var(--text-primary)',
};
