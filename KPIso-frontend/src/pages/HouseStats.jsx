import { useState, useContext, useMemo } from 'react';
import { AuthContext } from '../context/authContextValue.js';
import { useHouseStats } from '../hooks/useHouseStats';
import { formatCurrency } from '../utils/formatCurrency';
import MonthlyEvolutionChart from '../components/stats/MonthlyEvolutionChart';
import TopExpensesTable from '../components/stats/TopExpensesTable';
import TopProductsTable from '../components/stats/TopProductsTable';
import ConvivenciaKpiChart from '../components/stats/ConvivenciaKpiChart';
import { Card } from '../components/ui/Card.jsx';

// ── Icono local ──────────────────────────────────────────────────────────────
const IconPerson = () => (
    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="7" r="4" />
        <path d="M4 21v-1a8 8 0 0 1 16 0v1" />
    </svg>
);

// ── Utilidades ───────────────────────────────────────────────────────────────

/**
 * Genera las opciones del selector de mes de forma dinámica:
 * - "Histórico Total" → value = 'total'
 * - Últimos 12 meses → value = 'YYYY-MM' (descendente, mes actual primero)
 */
function buildMonthOptions() {
    const options = [{ value: 'total', label: 'Histórico Total' }];
    const now = new Date();
    for (let i = 0; i < 12; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const year = d.getFullYear();
        const month = d.getMonth() + 1;
        const value = `${year}-${String(month).padStart(2, '0')}`;
        // "junio 2026" → "Junio 2026"
        const rawLabel = d.toLocaleString('es-ES', { month: 'long', year: 'numeric' });
        const label = rawLabel.charAt(0).toUpperCase() + rawLabel.slice(1);
        options.push({ value, label });
    }
    return options;
}

/**
 * HouseStats — Panel de estadísticas de la casa.
 *
 * Props:
 *   houseId  — UUID de la casa.
 *   members  — Array de { userId, username, ... } proveniente de house.members (HouseDetail).
 *              Se usa para resolver memberId → username en los datos del backend.
 *
 * Muestra:
 *   1. Coste de vida por persona      → tarjetas glassmorphism
 *   2. Evolución mensual de gastos    → gráfico de línea (Recharts)
 *   3. Top 10 gastos más caros        → tabla
 *   4. Top 5 productos más recurrentes/caros → 2 tablas
 *   5. Rendimiento de convivencia KPI → gráfico de barras (Recharts)
 *
 * El selector de mes lista dinámicamente los últimos 12 meses + "Histórico Total".
 * Todos los datos monetarios excluyen DIRECT_PAYMENT (gestionado en el backend).
 * Sin caché: cada cambio de mes lanza una nueva petición.
 */
export default function HouseStats({ houseId, members = [] }) {
    const authContext = useContext(AuthContext);

    // Selector de mes — por defecto el mes actual ('YYYY-MM')
    const now = new Date();
    const currentMonthValue = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const [selectedMonth, setSelectedMonth] = useState(currentMonthValue);

    // Opciones generadas dinámicamente (memoizadas)
    const monthOptions = useMemo(() => buildMonthOptions(), []);

    // Mapa memberId (UUID) → username, construido a partir de house.members
    // El backend devuelve Maps con key = UUID del HouseMember (no del User).
    // house.members tiene { userId, username } donde userId = User.id.
    // Para cubrir ambos casos intentamos mapear tanto userId como el id del miembro si existe.
    const memberNameMap = useMemo(() => {
        const map = {};
        members.forEach((m) => {
            // userId es el campo del User; id sería el del HouseMember si se expone
            if (m.userId) map[m.userId] = m.username;
            if (m.id)     map[m.id]     = m.username;
        });
        return map;
    }, [members]);

    /** Resuelve un UUID a nombre legible, con fallback a "Miembro XXXX". */
    const resolveName = (uuid) =>
        memberNameMap[uuid] ?? `Miembro ${String(uuid).slice(0, 6)}…`;

    // Hook que dispara la petición real al backend
    const { data, loading, error } = useHouseStats(houseId, selectedMonth);

    // ── Extraer y normalizar los campos del DTO del backend ──────────────────
    const livingCostPerMember = data?.livingCostPerMember ?? {};   // Map<UUID, BigDecimal>
    const monthlyEvolution    = data?.monthlyExpenseEvolution ?? []; // [{ month, total }]
    const topExpenses         = data?.topExpenses ?? [];             // [{ id, description, amount, date, memberId }]
    const productStats        = data?.productStats ?? {};            // { topFrequentProducts, topExpensiveProducts }
    const taskKpiPoints       = data?.taskKpiPoints ?? {};           // Map<UUID, Integer>

    // 1. Coste de vida — enriquecido con nombre del miembro
    const memberCostEntries = Object.entries(livingCostPerMember).map(([memberId, amount]) => ({
        memberId,
        username: resolveName(memberId),
        amount: Number(amount),
    }));

    // 3. Top expenses — enriquecidas con nombre del miembro
    const enrichedExpenses = topExpenses.map((e) => ({
        ...e,
        memberName: resolveName(e.memberId),
    }));

    // 5. KPI — enriquecido con nombre del miembro, ordenado descendente
    const kpiData = Object.entries(taskKpiPoints)
        .map(([memberId, points]) => ({
            memberId,
            username: resolveName(memberId),
            points: Number(points),
        }))
        .sort((a, b) => b.points - a.points);

    // ── Render ───────────────────────────────────────────────────────────────
    return (
        <section
            style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 'var(--space-6)',
                padding: 'var(--space-4)',
            }}
        >
            {/* ── Cabecera con selector de mes ─────────────────────────────── */}
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 'var(--space-4)',
                    flexWrap: 'wrap',
                }}
            >
                <h2
                    style={{
                        margin: 0,
                        fontSize: 'var(--text-xl)',
                        fontWeight: 'var(--font-bold)',
                        color: 'var(--text-primary)',
                    }}
                >
                    Estadísticas de la casa
                </h2>

                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', marginLeft: 'auto' }}>
                    <label
                        htmlFor="houseStats-monthSelect"
                        style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}
                    >
                        Período:
                    </label>
                    <select
                        id="houseStats-monthSelect"
                        value={selectedMonth}
                        onChange={(e) => setSelectedMonth(e.target.value)}
                        style={{
                            padding: 'var(--space-2) var(--space-3)',
                            borderRadius: 'var(--radius-md)',
                            border: '1px solid var(--glass-border)',
                            background: 'var(--glass-bg)',
                            color: 'var(--text-primary)',
                            fontSize: 'var(--text-sm)',
                            cursor: 'pointer',
                            backdropFilter: 'var(--glass-blur)',
                            outline: 'none',
                        }}
                    >
                        {monthOptions.map((opt) => (
                            <option key={opt.value} value={opt.value} style={{ background: 'var(--bg-surface)', color: 'var(--text-primary)' }}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* ── Estado de carga ──────────────────────────────────────────── */}
            {loading && (
                <div style={{ textAlign: 'center', padding: 'var(--space-12)', color: 'var(--text-secondary)' }}>
                    Cargando estadísticas…
                </div>
            )}

            {/* ── Estado de error ──────────────────────────────────────────── */}
            {error && !loading && (
                <div
                    style={{
                        padding: 'var(--space-4)',
                        borderRadius: 'var(--radius-md)',
                        background: 'color-mix(in srgb, var(--danger, #f87171) 15%, transparent)',
                        color: 'var(--danger, #f87171)',
                        border: '1px solid color-mix(in srgb, var(--danger, #f87171) 30%, transparent)',
                    }}
                >
                    Error al cargar las estadísticas. Inténtalo de nuevo.
                </div>
            )}

            {/* ── Contenido (solo cuando hay datos) ───────────────────────── */}
            {!loading && !error && data && (
                <Card
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 'var(--space-6)',
                        padding: 'var(--space-6)',
                    }}
                >
                    {/* 1. Coste de vida por persona */}
                    <div>
                        <h3 style={sectionTitleStyle}>Coste de vida por persona</h3>
                        <div
                            style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                                gap: 'var(--space-3)',
                            }}
                        >
                            {memberCostEntries.length === 0 ? (
                                <p style={{ color: 'var(--text-secondary)' }}>Sin gastos registrados.</p>
                            ) : (
                                memberCostEntries.map(({ memberId, username, amount }) => (
                                    <div key={memberId} style={glassCardStyle}>
                                        <span style={{ fontSize: 'var(--text-lg)', color: 'var(--accent)', marginBottom: 'var(--space-1)' }}>
                                            <IconPerson />
                                        </span>
                                        <span
                                            style={{
                                                fontSize: 'var(--text-sm)',
                                                fontWeight: 'var(--font-semibold)',
                                                color: 'var(--text-primary)',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                                maxWidth: '100%',
                                            }}
                                            title={username}
                                        >
                                            {username}
                                        </span>
                                        <span
                                            style={{
                                                fontSize: 'var(--text-xl)',
                                                fontWeight: 'var(--font-bold)',
                                                color: 'var(--text-primary)',
                                                marginTop: 'var(--space-1)',
                                            }}
                                        >
                                            {formatCurrency(amount)}
                                        </span>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* 2. Evolución mensual */}
                    <div>
                        <h3 style={sectionTitleStyle}>Evolución mensual de gastos</h3>
                        <div
                            style={{
                                height: 300,
                                borderRadius: 'var(--radius-lg)',
                                background: 'var(--bg-glass)',
                                backdropFilter: 'blur(12px)',
                                border: '1px solid var(--border-subtle)',
                                padding: 'var(--space-4)',
                            }}
                        >
                            <MonthlyEvolutionChart data={monthlyEvolution} />
                        </div>
                    </div>

                    {/* 3. Top 10 gastos */}
                    <div>
                        <h3 style={sectionTitleStyle}>Top 10 gastos más caros</h3>
                        <TopExpensesTable expenses={enrichedExpenses} />
                    </div>

                    {/* 4. Top 5 productos */}
                    <div>
                        <h3 style={sectionTitleStyle}>Hábitos de compra</h3>
                        <div
                            style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                                gap: 'var(--space-4)',
                            }}
                        >
                            <TopProductsTable
                                title="Top 5 más recurrentes"
                                rows={productStats.topFrequentProducts ?? []}
                            />
                            <TopProductsTable
                                title="Top 5 más caros"
                                rows={productStats.topExpensiveProducts ?? []}
                            />
                        </div>
                    </div>

                    {/* 5. Rendimiento de convivencia – KPI */}
                    <div>
                        <h3 style={sectionTitleStyle}>Rendimiento de convivencia (Puntos KPI)</h3>
                        <div
                            style={{
                                height: Math.max(220, kpiData.length * 60),
                                borderRadius: 'var(--radius-lg)',
                                background: 'var(--bg-glass)',
                                backdropFilter: 'blur(12px)',
                                border: '1px solid var(--border-subtle)',
                                padding: 'var(--space-4)',
                            }}
                        >
                            <ConvivenciaKpiChart data={kpiData} />
                        </div>
                    </div>
                </Card>
            )}
        </section>
    );
}

// ── Estilos compartidos ───────────────────────────────────────────────────────
const sectionTitleStyle = {
    margin: '0 0 var(--space-3) 0',
    fontSize: 'var(--text-sm)',
    fontWeight: 'var(--font-bold)',
    color: 'var(--text-secondary)',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
};

const glassCardStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 'var(--space-4)',
    borderRadius: 'var(--radius-lg)',
    background: 'var(--bg-glass)',
    backdropFilter: 'blur(12px)',
    border: '1px solid var(--border-subtle)',
    textAlign: 'center',
    gap: 'var(--space-1)',
};
