import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
    ResponsiveContainer,
} from 'recharts';
import { formatCurrency } from '../../utils/formatCurrency';

/**
 * MonthlyEvolutionChart — Gráfico de línea con la evolución mensual del gasto total.
 *
 * Props:
 *   data: Array de { month: 'YYYY-MM', total: number }
 *
 * Usa variables CSS globales para respetar glassmorphism y modo oscuro/claro.
 */
export default function MonthlyEvolutionChart({ data = [] }) {
    // Transformar 'YYYY-MM' a etiqueta legible en español (ej: "ene. 2026")
    const formatted = data
        .filter((d) => d && typeof d.yearMonth === 'string' && d.yearMonth.includes('-'))
        .map((d) => {
            const [year, month] = d.yearMonth.split('-');
            const date = new Date(Number(year), Number(month) - 1, 1);
            return {
                mes: date.toLocaleString('es-ES', { month: 'short', year: 'numeric' }),
                totalAmount: Number(d.totalAmount || 0),
            };
        });

    if (formatted.length === 0) {
        return (
            <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 'var(--space-8)' }}>
                Sin datos para el período seleccionado.
            </div>
        );
    }

    return (
        <ResponsiveContainer width="100%" height="100%">
            <LineChart data={formatted} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" />
                <XAxis
                    dataKey="mes"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                />
                <YAxis
                    tickFormatter={(value) => formatCurrency(value)}
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                    width={90}
                />
                <Tooltip
                    formatter={(value) => [formatCurrency(value), 'Gasto total']}
                    labelFormatter={(label) => `Mes: ${label}`}
                    contentStyle={{
                        background: 'var(--bg-glass)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--text-primary)',
                        backdropFilter: 'blur(12px)',
                    }}
                />
                <Line
                    type="monotone"
                    dataKey="totalAmount"
                    stroke="var(--accent)"
                    strokeWidth={3}
                    dot={{ r: 4, fill: 'var(--accent)', stroke: 'var(--bg-surface)', strokeWidth: 2 }}
                    activeDot={{ r: 6 }}
                />
            </LineChart>
        </ResponsiveContainer>
    );
}
