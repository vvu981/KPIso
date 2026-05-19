import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/client';
import { Button } from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Input.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { ThemeToggle } from '../components/layout/Navbar.jsx';
import { IconSun, IconMoon } from '../components/ui/Icons.jsx';
import logo from '../assets/favicon.svg';

function buildRegisterPayload(formData) {
    const profilePictureUrl = formData.profilePictureUrl.trim();

    return {
        email: formData.email.trim(),
        username: formData.username.trim(),
        password: formData.password,
        profilePictureUrl: profilePictureUrl.length > 0 ? profilePictureUrl : null,
    };
}

function extractRegisterErrorMessage(errorResponse) {
    if (typeof errorResponse === 'string') {
        return errorResponse;
    }

    if (Array.isArray(errorResponse?.errors)) {
        return errorResponse.errors.join('\n');
    }

    if (errorResponse?.errors && typeof errorResponse.errors === 'object') {
        const fieldErrors = Object.entries(errorResponse.errors)
            .map(([field, message]) => `${field}: ${message}`);

        if (fieldErrors.length > 0) {
            return fieldErrors.join(' · ');
        }
    }

    if (typeof errorResponse?.message === 'string' && errorResponse.message.trim().length > 0) {
        return errorResponse.message;
    }

    if (typeof errorResponse?.error === 'string' && errorResponse.error.trim().length > 0) {
        return errorResponse.error;
    }

    return 'Error en el registro. Verifica los datos.';
}

/**
 * Register — Pantalla de creación de cuenta
 *
 * Principio S: responsabilidad única de registro de nuevo usuario.
 * Principio D: depende de abstracciones (api client) no de implementaciones.
 */
export default function Register({ isDark, onToggleTheme }) {
    const [formData, setFormData] = useState({
        email: '',
        username: '',
        password: '',
        profilePictureUrl: '',
    });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleChange = (field) => (e) => {
        setFormData(prev => ({ ...prev, [field]: e.target.value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);
        try {
            const payload = buildRegisterPayload(formData);
            await api.post('/auth/register', payload);
            setSuccess('¡Registro completado! Redirigiendo al inicio de sesión...');
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(extractRegisterErrorMessage(err.response?.data));
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
                        Únete a KPIso
                    </h1>

                    <p style={{
                        fontSize: 'var(--text-lg)',
                        color: 'var(--text-secondary)',
                        lineHeight: 'var(--leading-relaxed)',
                        marginBottom: 'var(--space-8)',
                    }}>
                        Crea tu cuenta y empieza a gestionar la convivencia de tu hogar de forma inteligente y transparente.
                    </p>

                    {/* Steps visuales */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', textAlign: 'left' }}>
                        {[
                            { num: '1', text: 'Crea tu cuenta gratis' },
                            { num: '2', text: 'Funda o únete a un piso' },
                            { num: '3', text: 'Gestiona tareas y gastos' },
                        ].map(step => (
                            <div key={step.num} style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 'var(--space-3)',
                                background: 'var(--glass-bg)',
                                border: '1px solid var(--glass-border)',
                                borderRadius: 'var(--radius-md)',
                                padding: 'var(--space-3) var(--space-4)',
                                backdropFilter: 'var(--glass-blur)',
                            }}>
                                <span style={{ 
                                    display: 'flex', 
                                    alignItems: 'center', 
                                    justifyContent: 'center', 
                                    width: '24px', 
                                    height: '24px', 
                                    background: 'var(--accent)', 
                                    color: 'white', 
                                    borderRadius: '50%', 
                                    fontSize: '12px', 
                                    fontWeight: 'bold' 
                                }}>{step.num}</span>
                                <span style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)', fontWeight: 500 }}>
                                    {step.text}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* ── PANEL DERECHO: FORMULARIO ── */}
            <div className="auth-form-panel">
                <div style={{ width: '100%', maxWidth: 400 }} className="anim-fade-in">

                    <div style={{ marginBottom: 'var(--space-8)' }}>
                        <h2 style={{
                            fontSize: 'var(--text-3xl)',
                            fontWeight: 800,
                            letterSpacing: '-0.03em',
                            color: 'var(--text-primary)',
                            marginBottom: 'var(--space-2)',
                        }}>
                            Crear cuenta
                        </h2>
                        <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                            Rellena el formulario para empezar
                        </p>
                    </div>

                    <Alert type="error" message={error} />
                    <Alert type="success" message={success} />

                    <form
                        onSubmit={handleSubmit}
                        style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 'var(--space-4)',
                            marginTop: (error || success) ? 'var(--space-4)' : 0,
                        }}
                    >
                        <Input
                            id="reg-username"
                            label="Nombre de usuario"
                            type="text"
                            value={formData.username}
                            onChange={handleChange('username')}
                            placeholder="@tuusuario"
                            required
                        />
                        <Input
                            id="reg-email"
                            label="Correo electrónico"
                            type="email"
                            value={formData.email}
                            onChange={handleChange('email')}
                            placeholder="tu@email.com"
                            required
                        />
                        <Input
                            id="reg-password"
                            label="Contraseña"
                            type="password"
                            value={formData.password}
                            onChange={handleChange('password')}
                            placeholder="••••••••"
                            required
                        />
                        <Input
                            id="reg-pic"
                            label="URL foto de perfil"
                            type="text"
                            value={formData.profilePictureUrl}
                            onChange={handleChange('profilePictureUrl')}
                            placeholder="https://... (opcional)"
                            hint="Pega una URL pública de imagen o déjalo vacío"
                        />

                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            full
                            loading={loading}
                        >
                            {!loading && 'Crear cuenta →'}
                        </Button>
                    </form>

                    <div className="divider-text" style={{ margin: 'var(--space-6) 0' }}>o</div>

                    <div style={{
                        textAlign: 'center',
                        fontSize: 'var(--text-sm)',
                        color: 'var(--text-secondary)',
                    }}>
                        ¿Ya tienes cuenta?{' '}
                        <Link to="/login" style={{ fontWeight: 700 }}>
                            Inicia sesión
                        </Link>
                    </div>
                </div>
            </div>

        </div>
    );
}