import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
    ResponsiveContainer,
    Legend
} from 'recharts';

/**
 * MonthlyKpiChart — Gráfico de barras agrupadas que muestra la evolución de puntos KPI de cada usuario por mes.
 */
export default function MonthlyKpiChart({ data = [], members = [], memberStatuses = {} }) {
    const formattedData = data.map((item) => {
        if (!item.month) return item;
        const [year, month] = item.month.split('-');
        const date = new Date(Number(year), Number(month) - 1, 1);
        const mes = date.toLocaleString('es-ES', { month: 'short', year: 'numeric' });
        
        return {
            ...item,
            mes,
        };
    });

    if (formattedData.length === 0) {
        return (
            <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 'var(--space-8)' }}>
                Sin datos de KPI mensuales para mostrar.
            </div>
        );
    }

    const COLORS = [
        'var(--accent)',
        'var(--cyan, #06b6d4)',
        'var(--success, #10b981)',
        'var(--warning, #f59e0b)',
        'var(--danger, #ef4444)'
    ];

    return (
        <ResponsiveContainer width="100%" height="100%">
            <BarChart data={formattedData} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" />
                <XAxis
                    dataKey="mes"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                />
                <YAxis
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                    allowDecimals={false}
                    width={40}
                />
                <Tooltip
                    labelFormatter={(label) => `Mes: ${label}`}
                    contentStyle={{
                        background: 'var(--bg-glass)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--text-primary)',
                        backdropFilter: 'blur(12px)',
                    }}
                />
                <Legend 
                    wrapperStyle={{ fontSize: 12, paddingTop: 10 }}
                />
                {members.map((member, idx) => {
                    const userColor = memberStatuses[member.userId]?.color || COLORS[idx % COLORS.length];
                    return (
                        <Bar
                            key={member.userId}
                            dataKey={member.userId}
                            name={member.username}
                            fill={userColor}
                            radius={[4, 4, 0, 0]}
                        />
                    );
                })}
            </BarChart>
        </ResponsiveContainer>
    );
}
