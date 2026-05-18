/**
 * useTheme — Hook para gestión del tema dark/light
 *
 * Principio S: responsabilidad única de persistencia y
 * aplicación del tema en el DOM.
 * Principio O: extensible para añadir más temas sin modificar
 * los consumidores del hook.
 */
import { useState, useEffect, useCallback } from 'react';

const THEME_KEY = 'kpiso-theme';
const DARK = 'dark';
const LIGHT = 'light';

/**
 * Lee el tema preferido: localStorage → preferencia del sistema → dark por defecto
 */
function getInitialTheme() {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === DARK || stored === LIGHT) return stored;
    if (window.matchMedia?.('(prefers-color-scheme: light)').matches) return LIGHT;
    return DARK;
}

/**
 * Aplica el atributo data-theme en el elemento raíz del DOM
 */
function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
}

export function useTheme() {
    const [theme, setTheme] = useState(getInitialTheme);

    // Aplicar en el DOM cuando cambia
    useEffect(() => {
        applyTheme(theme);
        localStorage.setItem(THEME_KEY, theme);
    }, [theme]);

    // Inicializar en el primer render
    useEffect(() => {
        applyTheme(getInitialTheme());
    }, []);

    const toggleTheme = useCallback(() => {
        setTheme(prev => (prev === DARK ? LIGHT : DARK));
    }, []);

    const isDark = theme === DARK;

    return { theme, isDark, toggleTheme };
}
