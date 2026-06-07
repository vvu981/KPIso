import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
    ResponsiveContainer,
    Cell,
    Legend
} from 'recharts';

/**
 * ConvivenciaKpiChart — Gráfico de barras horizontales agrupadas con los puntos KPI
 * obtenidos vs asignados de cada usuario.
 */
export default function ConvivenciaKpiChart({ data = [], memberStatuses = {} }) {
    if (data.length === 0) {
        return (
            <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 'var(--space-8)' }}>
                Sin datos de KPI para el período seleccionado.
            </div>
        );
    }

    const COLORS = [
        'var(--accent)',
        'var(--cyan, #06b6d4)',
        'var(--success, #10b981)',
        'var(--warning, #f59e0b)',
        'var(--danger, #ef4444)',
    ];

    // Tooltip personalizado para mostrar la comparación de forma clara
    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            const obtenidos = payload.find(p => p.dataKey === 'puntosLleva')?.value ?? 0;
            const asignados = payload.find(p => p.dataKey === 'puntosDebe')?.value ?? 0;
            const porcentaje = asignados > 0 ? Math.round((obtenidos / asignados) * 100) : 0;

            return (
                <div
                    style={{
                        background: 'var(--bg-glass)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        padding: 'var(--space-3)',
                        backdropFilter: 'blur(12px)',
                        color: 'var(--text-primary)',
                        fontSize: '13px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '4px'
                    }}
                >
                    <div style={{ fontWeight: 'var(--font-bold)', marginBottom: '4px' }}>{label}</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 'var(--space-4)' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>Obtenidos:</span>
                        <span style={{ fontWeight: 'var(--font-semibold)' }}>{obtenidos} pts</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 'var(--space-4)' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>Asignados:</span>
                        <span style={{ fontWeight: 'var(--font-semibold)' }}>{asignados} pts</span>
                    </div>
                    {asignados > 0 && (
                        <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '4px', marginTop: '4px', fontSize: '11px', color: 'var(--accent-light)' }}>
                            Progreso: {porcentaje}% completado
                        </div>
                    )}
                </div>
            );
        }
        return null;
    };

    const renderCustomLegend = (props) => {
        const { payload } = props;
        return (
            <div style={{ display: 'flex', gap: 'var(--space-4)', justifyContent: 'center', marginTop: 'var(--space-2)', flexWrap: 'wrap' }}>
                {payload.map((entry, index) => {
                    const isObtenidos = entry.dataKey === 'puntosLleva';
                    return (
                        <div key={`item-${index}`} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                            <svg width="24" height="8" style={{ display: 'inline-block', verticalAlign: 'middle' }}>
                                {isObtenidos ? (
                                    <line x1="0" y1="4" x2="24" y2="4" stroke="var(--accent)" strokeWidth="4" />
                                ) : (
                                    <line x1="0" y1="4" x2="24" y2="4" stroke="var(--accent)" strokeWidth="4" strokeDasharray="4 2" />
                                )}
                            </svg>
                            <span style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>
                                {entry.value}
                            </span>
                        </div>
                    );
                })}
            </div>
        );
    };

    return (
        <ResponsiveContainer width="100%" height="100%">
            <BarChart
                data={data}
                layout="vertical"
                margin={{ top: 8, right: 40, left: 8, bottom: 8 }}
                barGap={2}
            >
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" horizontal={false} />
                <XAxis
                    type="number"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                    allowDecimals={false}
                />
                <YAxis
                    type="category"
                    dataKey="username"
                    stroke="var(--text-secondary)"
                    tick={{ fontSize: 13, fill: 'var(--text-primary)' }}
                    width={100}
                />
                <Tooltip content={<CustomTooltip />} />
                <Legend content={renderCustomLegend} />
                 <Bar 
                    dataKey="puntosLleva" 
                    name="Puntos Obtenidos" 
                    fill="var(--accent)"
                    stroke="var(--accent)"
                    strokeWidth={1.5}
                    radius={[0, 4, 4, 0]} 
                    maxBarSize={15}
                >
                    {data.map((entry, idx) => {
                        const color = memberStatuses[entry.memberId]?.color || COLORS[idx % COLORS.length];
                        return <Cell key={idx} fill={color} stroke={color} strokeWidth={1.5} />;
                    })}
                </Bar>
                <Bar 
                    dataKey="puntosDebe" 
                    name="Puntos Asignados" 
                    fill="var(--accent-ultra-light)"
                    stroke="var(--accent)"
                    strokeWidth={1.5}
                    strokeDasharray="3 2"
                    radius={[0, 4, 4, 0]} 
                    maxBarSize={15}
                >
                    {data.map((entry, idx) => {
                        const color = memberStatuses[entry.memberId]?.color || COLORS[idx % COLORS.length];
                        return (
                            <Cell 
                                key={idx} 
                                fill={color} 
                                fillOpacity={0.25} 
                                stroke={color} 
                                strokeWidth={1.5}
                                strokeDasharray="3 2"
                            />
                        );
                    })}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}
