import api from '../api/client';

/**
 * houseStatsService — Wrapper around the backend stats endpoint.
 *
 * GET /houses/{houseId}/stats?month={month}&userId={userId}
 *
 * `month` puede ser:
 *   - 'YYYY-MM' → mes específico (ej: '2026-05')
 *   - null/undefined → el backend asume el mes actual
 *
 * El backend siempre excluye los movimientos de tipo DIRECT_PAYMENT.
 * Devuelve la estructura completa de HouseStatsResponse.
 */
export const getHouseStats = async (houseId, userId, month = null) => {
    const params = new URLSearchParams();
    if (userId) params.append('userId', userId);
    if (month) params.append('month', month);
    const response = await api.get(`/houses/${houseId}/stats?${params.toString()}`);
    return response.data;
};
