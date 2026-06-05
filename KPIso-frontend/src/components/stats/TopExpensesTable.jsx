import { formatCurrency } from '../../utils/formatCurrency';

/**
 * TopExpensesTable — Tabla con los 10 tickets de mayor importe.
 * Sin paginación: el backend ya limita a LIMIT 10.
 *
 * Props:
 *   expenses: Array de {
 *     id: string,
 *     description: string,
 *     amount: number,
 *     date: string (ISO),
 *     memberId: string,
 *     memberName?: string,  // enriquecido en HouseStats si se dispone de members
 *   }
 */
export default function TopExpensesTable({ expenses = [] }) {
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
                Top 10 gastos más caros
            </h3>

            {expenses.length === 0 ? (
                <p style={{ padding: 'var(--space-4)', color: 'var(--text-secondary)', textAlign: 'center' }}>
                    Sin datos para el período seleccionado.
                </p>
            ) : (
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 'var(--text-sm)' }}>
                        <thead>
                            <tr style={{ background: 'var(--bg-soft)' }}>
                                <th style={thStyle}>#</th>
                                <th style={thStyle}>Concepto</th>
                                <th style={{ ...thStyle, textAlign: 'right' }}>Importe</th>
                                <th style={thStyle}>Fecha</th>
                                <th style={thStyle}>Miembro</th>
                            </tr>
                        </thead>
                        <tbody>
                            {expenses.map((e, idx) => (
                                <tr
                                    key={e.id ?? idx}
                                    style={{
                                        borderBottom: '1px solid var(--border-subtle)',
                                        transition: 'background 150ms',
                                    }}
                                    onMouseEnter={(ev) => (ev.currentTarget.style.background = 'var(--bg-soft)')}
                                    onMouseLeave={(ev) => (ev.currentTarget.style.background = 'transparent')}
                                >
                                    <td style={{ ...tdStyle, color: 'var(--text-secondary)', width: 32 }}>{idx + 1}</td>
                                    <td style={tdStyle}>{e.description ?? '—'}</td>
                                    <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 'var(--font-bold)', color: 'var(--text-primary)' }}>
                                        {formatCurrency(e.amount)}
                                    </td>
                                    <td style={{ ...tdStyle, color: 'var(--text-secondary)' }}>
                                        {e.date ? new Date(e.date).toLocaleDateString('es-ES') : '—'}
                                    </td>
                                    <td style={{ ...tdStyle, color: 'var(--text-secondary)' }}>
                                        {e.memberName ?? '—'}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
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
    whiteSpace: 'nowrap',
};

const tdStyle = {
    padding: 'var(--space-2) var(--space-3)',
    color: 'var(--text-primary)',
    whiteSpace: 'nowrap',
};
