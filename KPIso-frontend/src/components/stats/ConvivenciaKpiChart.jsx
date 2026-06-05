import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
    ResponsiveContainer,
    Cell,
} from 'recharts';

/**
 * ConvivenciaKpiChart — Gráfico de barras horizontales con los puntos KPI de cada usuario.
 *
 * Props:
 *   data: Array de { username: string, points: number }
 *         (pre-procesado en HouseStats: se fusionan members + taskKpiPoints del backend)
 *
 * Usa variables CSS globales para adaptarse automáticamente al modo oscuro/claro.
 */
export default function ConvivenciaKpiChart({ data = [] }) {
    if (data.length === 0) {
        return (
            <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 'var(--space-8)' }}>
                Sin datos de KPI para el período seleccionado.
            </div>
        );
    }

    // Paleta basada en el acento con variaciones de opacidad para distinguir barras
    const COLORS = [
        'var(--accent)',
        'var(--accent-soft, var(--accent))',
        'var(--success, #34d399)',
        'var(--warning, #fbbf24)',
        'var(--danger, #f87171)',
    ];

    return (
        <ResponsiveContainer width="100%" height="100%">
            <BarChart
                data={data}
                layout="vertical"
                margin={{ top: 8, right: 40, left: 8, bottom: 8 }}
            >
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" horizontal={false} />
                <XAxis
                    type="number"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                    allowDecimals={false}
                    label={{
                        value: 'Puntos KPI',
                        position: 'insideBottom',
                        offset: -4,
                        fill: 'var(--text-secondary)',
                        fontSize: 12,
                    }}
                />
                <YAxis
                    type="category"
                    dataKey="username"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 13, fill: 'var(--text-primary)' }}
                    width={120}
                />
                <Tooltip
                    formatter={(value) => [`${value} pts`, 'Puntos KPI']}
                    labelFormatter={(label) => `Usuario: ${label}`}
                    contentStyle={{
                        background: 'var(--bg-glass)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--text-primary)',
                        backdropFilter: 'blur(12px)',
                    }}
                />
                <Bar dataKey="points" radius={[0, 4, 4, 0]} maxBarSize={40}>
                    {data.map((_, idx) => (
                        <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}
