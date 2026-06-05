/**
 * formatCurrency — Formatea un número como moneda Euro española.
 * Salida: "12,34 €" (es-ES locale, 2 decimales fijos).
 */
export function formatCurrency(value) {
    const formatter = new Intl.NumberFormat('es-ES', {
        style: 'currency',
        currency: 'EUR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
    return formatter.format(value ?? 0);
}
