import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { useContext, useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext.jsx';
import { AuthContext } from './context/authContextValue.js';
import { Navbar } from './components/layout/Navbar';
import Dashboard from './pages/Dashboard';
import HouseDetail from './pages/HouseDetail';
import Login from './pages/Login';
import Register from './pages/Register';

/**
 * AppContent — Contenedor principal con navegación y enrutamiento
 *
 * Gestiona:
 * - Estado de tema (dark/light)
 * - Navegación global
 * - Logout
 * - Enrutamiento entre páginas
 */
function AppContent() {
    const auth = useContext(AuthContext);
    const [isDark, setIsDark] = useState(() => {
        const saved = localStorage.getItem('kpiso-theme');
        return saved ? saved === 'dark' : false;
    });

    // Aplicar tema al elemento root y persistir preferencia
    useEffect(() => {
        const root = document.documentElement;
        root.setAttribute('data-theme', isDark ? 'dark' : 'light');
        localStorage.setItem('kpiso-theme', isDark ? 'dark' : 'light');
    }, [isDark]);

    const handleToggleTheme = () => {
        setIsDark((prev) => !prev);
    };

    const handleLogout = () => {
        auth?.logoutUser?.();
    };

    return (
        <div
            style={{
                minHeight: '100vh',
                display: 'flex',
                flexDirection: 'column',
                backgroundColor: 'var(--bg-base)',
                color: 'var(--text-primary)',
                transition: 'background-color 200ms var(--timing-smooth)',
            }}
        >
            {/* Navbar Global */}
            {auth?.username && (
                <Navbar
                    username={auth.username}
                    email={auth.email}
                    profilePictureUrl={auth.profilePictureUrl}
                    isDark={isDark}
                    onToggleTheme={handleToggleTheme}
                    onLogout={handleLogout}
                />
            )}

            {/* Contenido Principal */}
            <main
                className="flex-1"
                style={{
                    paddingTop: 'var(--navbar-height)',
                    paddingBottom: 'var(--space-8)',
                }}
            >
                <Routes>
                    <Route path="/login" element={<Login isDark={isDark} onToggleTheme={handleToggleTheme} />} />
                    <Route path="/register" element={<Register isDark={isDark} onToggleTheme={handleToggleTheme} />} />
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/house/:houseId" element={<HouseDetail />} />
                </Routes>
            </main>
        </div>
    );
}

/**
 * App — Componente raíz
 *
 * Proporciona:
 * - AuthProvider para contexto de autenticación
 * - Router para enrutamiento
 * - AppContent como contenedor principal
 */
export default function App() {
    return (
        <AuthProvider>
            <Router>
                <AppContent />
            </Router>
        </AuthProvider>
    );
}