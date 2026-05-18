import { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/authContextValue.js';
import api from '../api/client';
import { Button } from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Input.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { ThemeToggle } from '../components/layout/Navbar.jsx';
import { IconClipboardDocumentList, IconBanknotes, IconTrophy, IconCalendar, IconSun, IconMoon } from '../components/ui/Icons.jsx';
import logo from '../assets/favicon.svg';

/**
 * Login — Pantalla de inicio de sesión
 *
 * Layout: panel izquierdo (branding) + panel derecho (formulario)
 * Principio S: responsabilidad única de autenticación de usuario.
 * Principio D: depende de abstracciones (AuthContext, api) no de implementaciones.
 */
export default function Login({ isDark, onToggleTheme }) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { loginUser } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const response = await api.post('/auth/login', { email, password });
            loginUser(response.data.accessToken, response.data.userId, response.data.username, response.data.email, response.data.profilePictureUrl);
            navigate('/');
        // eslint-disable-next-line no-unused-vars
        } catch (err) {
            setError('Credenciales incorrectas o fallo en el servidor.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-layout">
            <div style={{ position: 'absolute', top: 'var(--space-4)', right: 'var(--space-4)', zIndex: 100, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ThemeToggle isDark={isDark} onToggleTheme={onToggleTheme} />
                <span
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        color: isDark ? 'var(--accent-light)' : 'var(--warning)',
                        flexShrink: 0,
                    }}
                    aria-hidden="true"
                >
                    {isDark ? <IconMoon /> : <IconSun />}
                </span>
            </div>

            {/* ── PANEL IZQUIERDO: BRANDING ── */}
            <div className="auth-brand-panel">
                <div style={{ position: 'relative', zIndex: 1, textAlign: 'center', maxWidth: 420 }}>
                    {/* Logo */}
                    <div style={{
                        margin: '0 auto var(--space-6)',
                        animation: 'float 4s ease-in-out infinite',
                        display: 'flex',
                        justifyContent: 'center'
                    }}>
                        <img src={logo} alt="KPIso Logo" style={{ width: '140px', height: 'auto' }} />
                    </div>

                    <h1 style={{
                        fontSize: 'var(--text-4xl)',
                        fontWeight: 900,
                        letterSpacing: '-0.04em',
                        lineHeight: 1.1,
                        marginBottom: 'var(--space-4)',
                    }} className="text-gradient">
                        KPIso
                    </h1>

                    <p style={{
                        fontSize: 'var(--text-lg)',
                        color: 'var(--text-secondary)',
                        lineHeight: 'var(--leading-relaxed)',
                        marginBottom: 'var(--space-8)',
                    }}>
                        La plataforma inteligente para gestionar la convivencia de tu hogar.
                        Tareas, gastos y transparencia en un solo lugar.
                    </p>

                    {/* Feature pills */}
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-2)', justifyContent: 'center' }}>
                        {[
                            { icon: <IconClipboardDocumentList />, text: 'Gestión de tareas' },
                            { icon: <IconBanknotes />, text: 'Control de gastos' },
                            { icon: <IconTrophy />, text: 'Ranking KPI' },
                            { icon: <IconCalendar />, text: 'Calendario' }
                        ].map((f, i) => (
                            <span key={i} className="badge badge-purple" style={{ padding: '6px 12px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                {f.icon} {f.text}
                            </span>
                        ))}
                    </div>
                </div>
            </div>

            {/* ── PANEL DERECHO: FORMULARIO ── */}
            <div className="auth-form-panel">
                <div style={{ width: '100%', maxWidth: 400 }} className="anim-fade-in">

                    {/* Header del formulario */}
                    <div style={{ marginBottom: 'var(--space-8)' }}>
                        <h2 style={{
                            fontSize: 'var(--text-3xl)',
                            fontWeight: 800,
                            letterSpacing: '-0.03em',
                            color: 'var(--text-primary)',
                            marginBottom: 'var(--space-2)',
                        }}>
                            Bienvenido de vuelta
                        </h2>
                        <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                            Inicia sesión para acceder a tu hogar
                        </p>
                    </div>

                    {/* Alert de error */}
                    <Alert type="error" message={error} />

                    {/* Formulario */}
                    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', marginTop: error ? 'var(--space-4)' : 0 }}>
                        <Input
                            id="login-email"
                            label="Correo electrónico"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="tu@email.com"
                            required
                        />

                        <div>
                            <Input
                                id="login-password"
                                label="Contraseña"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>

                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            full
                            loading={loading}
                        >
                            {!loading && 'Iniciar sesión →'}
                        </Button>
                    </form>

                    {/* Divider */}
                    <div className="divider-text" style={{ margin: 'var(--space-6) 0' }}>o</div>

                    {/* Link a registro */}
                    <div style={{
                        textAlign: 'center',
                        fontSize: 'var(--text-sm)',
                        color: 'var(--text-secondary)',
                    }}>
                        ¿No tienes cuenta?{' '}
                        <Link to="/register" style={{ fontWeight: 700 }}>
                            Regístrate gratis
                        </Link>
                    </div>
                </div>
            </div>

        </div>
    );
}