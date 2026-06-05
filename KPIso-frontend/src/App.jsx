import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useContext, useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext.jsx';
import { AuthContext } from './context/authContextValue.js';
import { Navbar } from './components/layout/Navbar';
import Dashboard from './pages/Dashboard';
import HouseDetail from './pages/HouseDetail';
import Login from './pages/Login';
import Register from './pages/Register';
import api from './api/client';

/**
 * ProtectedRoute — Protege las rutas que requieren inicio de sesión
 */
function ProtectedRoute({ children }) {
    const auth = useContext(AuthContext);
    if (!auth?.token) {
        return <Navigate to="/login" replace />;
    }
    return children;
}

/**
 * PublicRoute — Redirige al Dashboard si el usuario ya está autenticado
 */
function PublicRoute({ children }) {
    const auth = useContext(AuthContext);
    if (auth?.token) {
        return <Navigate to="/" replace />;
    }
    return children;
}

/**
 * AppContent — Contenedor principal con navegación y enrutamiento
 */
function AppContent() {
    const auth = useContext(AuthContext);
    const [isDark, setIsDark] = useState(() => {
        const saved = localStorage.getItem('kpiso-theme');
        return saved ? saved === 'dark' : false;
    });

    // Interceptar errores 401 Unauthorized (JWT expirado o inválido)
    useEffect(() => {
        const interceptor = api.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response && error.response.status === 401) {
                    auth?.logoutUser?.();
                    window.location.href = '/login';
                }
                return Promise.reject(error);
            }
        );
        return () => {
            api.interceptors.response.eject(interceptor);
        };
    }, [auth]);

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
                    paddingTop: auth?.username ? 0 : 'var(--navbar-height)',
                    paddingBottom: 'var(--space-8)',
                }}
            >
                <Routes>
                    <Route path="/login" element={<PublicRoute><Login isDark={isDark} onToggleTheme={handleToggleTheme} /></PublicRoute>} />
                    <Route path="/register" element={<PublicRoute><Register isDark={isDark} onToggleTheme={handleToggleTheme} /></PublicRoute>} />
                    <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
                    <Route path="/house/:houseId" element={<ProtectedRoute><HouseDetail /></ProtectedRoute>} />
                </Routes>
            </main>
        </div>
    );
}

/**
 * App — Componente raíz
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