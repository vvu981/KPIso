import { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/authContextValue.js';
import { getHouseStats } from '../services/houseStatsService';

/**
 * useHouseStats — Hook para obtener las estadísticas de una casa.
 *
 * El selector de mes acepta:
 *   - 'YYYY-MM'  → mes específico
 *   - null       → mes actual (backend default)
 *
 * Estructura esperada del backend (HouseStatsResponse):
 * {
 *   livingCostPerMember:    Map<UUID, BigDecimal>,   // coste por miembro
 *   monthlyExpenseEvolution: [{ month: 'YYYY-MM', total: number }],
 *   topExpenses:            [{ id, description, amount, date, memberId }],
 *   productStats: {
 *     topFrequentProducts: string[],
 *     topExpensiveProducts: string[],
 *   },
 *   taskKpiPoints:          Map<UUID, number>,       // puntos KPI por miembro
 * }
 *
 * Retorna { data, loading, error, refresh }.
 */
export function useHouseStats(houseId, selectedMonth) {
    const authContext = useContext(AuthContext);
    const userId = authContext?.user?.id ?? authContext?.userId ?? null;

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchStats = async (month) => {
        if (!houseId) return;
        setLoading(true);
        setError(null);
        try {
            // null → backend usa el mes actual; 'YYYY-MM' → mes específico
            const monthParam = month === 'total' ? null : month;
            const response = await getHouseStats(houseId, userId, monthParam);
            setData(response);
        } catch (err) {
            setError(err);
            setData(null);
        } finally {
            setLoading(false);
        }
    };

    // Cargar cada vez que cambie el mes o la casa
    useEffect(() => {
        fetchStats(selectedMonth);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [houseId, selectedMonth]);

    return { data, loading, error, refresh: () => fetchStats(selectedMonth) };
}
