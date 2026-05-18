import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/authContextValue.js';
import api from '../api/client';
import { Button } from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Input.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { Card } from '../components/ui/Card.jsx';
import { IconKey, IconArrowRightOnRectangle, IconCheckBadge } from '../components/ui/Icons.jsx';

/**
 * JoinHouse — Pantalla para unirse a un piso mediante código de invitación
 *
 * Principio S: responsabilidad única de gestionar el flujo de unión a una casa.
 * Principio D: depende de abstracciones (api, AuthContext).
 */
export default function JoinHouse() {
    const [inviteCode, setInviteCode] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const authContext = useContext(AuthContext);
    const { userId = null } = authContext || {};
    const navigate = useNavigate();

    useEffect(() => {
        if (!userId) navigate('/login', { replace: true });
    }, [userId, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);
        try {
            await api.post('/houses/join', { inviteCode, userId });
            setSuccess('¡Bienvenido al piso! Redirigiendo...');
            setTimeout(() => navigate('/'), 2000);
        } catch (err) {
            setError(err.response?.data || 'Código inválido o ya eres miembro de este hogar.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            minHeight: '100vh',
            background: 'var(--bg-base)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 'var(--space-6)',
        }}>
            <div style={{ width: '100%', maxWidth: 420 }} className="anim-fade-in">

                {/* Header */}
                <div style={{ textAlign: 'center', marginBottom: 'var(--space-8)' }}>
                    <div style={{
                        width: 60,
                        height: 60,
                        borderRadius: 'var(--radius-xl)',
                        background: 'var(--gradient-brand)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '1.75rem',
                        margin: '0 auto var(--space-4)',
                        boxShadow: '0 8px 24px var(--accent-glow)',
                    }}>
                        <IconKey />
                    </div>
                    <h1 style={{
                        fontSize: 'var(--text-2xl)',
                        fontWeight: 800,
                        color: 'var(--text-primary)',
                        letterSpacing: '-0.03em',
                        marginBottom: 'var(--space-2)',
                    }}>
                        Unirse a un Piso
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                        Introduce el código de 6 caracteres que te compartió tu compañero
                    </p>
                </div>

                <Card>
                    <Alert type="error" message={error} />
                    <Alert type="success" message={success} />

                    {!success && (
                        <form
                            onSubmit={handleSubmit}
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 'var(--space-5)',
                                marginTop: error ? 'var(--space-4)' : 0,
                            }}
                        >
                            {/* Input de código estilizado */}
                            <div className="form-group">
                                <label htmlFor="invite-code" className="form-label">
                                    Código de invitación
                                </label>
                                <input
                                    id="invite-code"
                                    type="text"
                                    required
                                    maxLength={6}
                                    className="form-input input-mono"
                                    value={inviteCode}
                                    onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                                    placeholder="AX93LD"
                                />
                                <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-tertiary)' }}>
                                    6 caracteres en mayúsculas, sin espacios
                                </span>
                            </div>

                            <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                                <Button
                                    variant="secondary"
                                    size="md"
                                    full
                                    onClick={() => navigate('/')}
                                    type="button"
                                >
                                    Cancelar
                                </Button>
                                <Button
                                    type="submit"
                                    variant="primary"
                                    size="md"
                                    full
                                    loading={loading}
                                    disabled={inviteCode.length < 6}
                                >
                                    {!loading && <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><IconArrowRightOnRectangle /> Unirse</span>}
                                </Button>
                            </div>
                        </form>
                    )}

                    {success && (
                        <div style={{ textAlign: 'center', padding: 'var(--space-4)', marginTop: 'var(--space-4)' }}>
                            <div style={{ fontSize: '4rem', color: 'var(--success)', marginBottom: 'var(--space-3)', animation: 'float 3s ease-in-out infinite' }}>
                                <IconCheckBadge />
                            </div>
                            <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                                Accediendo a tu nuevo hogar...
                            </p>
                        </div>
                    )}
                </Card>

                <div style={{ textAlign: 'center', marginTop: 'var(--space-5)' }}>
                    <Link to="/" style={{ fontSize: 'var(--text-sm)', color: 'var(--text-tertiary)' }}>
                        ← Volver al panel
                    </Link>
                </div>
            </div>
        </div>
    );
}