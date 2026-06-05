import { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/client';
import { AuthContext } from '../context/authContextValue.js';
import { Button } from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Input.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Modal } from '../components/ui/Modal.jsx';
import { IconHome, IconCheckBadge, IconHomePlus } from '../components/ui/Icons.jsx';
const IconCopy = () => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
    </svg>
);

const IconCheckMini = () => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--success, #10b981)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12"/>
    </svg>
);

/**
 * CreateHouse — Creación de nueva vivienda
 *
 * Principio S: responsabilidad única de gestionar el flujo de creación de casa.
 * Principio D: depende de abstracciones (api, AuthContext).
 */
export default function CreateHouse() {
    const [formData, setFormData] = useState({ name: '', profilePictureUrl: '' });
    const [createdHouse, setCreatedHouse] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [copied, setCopied] = useState(false);
    const authContext = useContext(AuthContext);
    const { userId = null } = authContext || {};
    const navigate = useNavigate();

    const handleChange = (field) => (e) => {
        setFormData(prev => ({ ...prev, [field]: e.target.value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const response = await api.post('/houses', {
                name: formData.name,
                profilePictureUrl: formData.profilePictureUrl,
                creatorId: userId,
            });
            setCreatedHouse(response.data);
        } catch (err) {
            setError(err.response?.data || 'No se pudo crear la casa. Verifica los datos.');
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
            <div style={{ width: '100%', maxWidth: 440 }} className="anim-fade-in">

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
                        <IconHome />
                    </div>
                    <h1 style={{
                        fontSize: 'var(--text-2xl)',
                        fontWeight: 800,
                        color: 'var(--text-primary)',
                        letterSpacing: '-0.03em',
                        marginBottom: 'var(--space-2)',
                    }}>
                        Fundar Nueva Casa
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                        Serás el administrador y podrás invitar a tus compañeros
                    </p>
                </div>

                <Card>
                    <Alert type="error" message={error} />

                    {createdHouse ? (
                        /* ── ESTADO DE ÉXITO ── */
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            gap: 'var(--space-5)',
                            padding: 'var(--space-4)',
                            textAlign: 'center',
                        }} className="anim-scale-in">
                            <div style={{ fontSize: '4rem', color: 'var(--success)', animation: 'float 3s ease-in-out infinite' }}>
                                <IconCheckBadge />
                            </div>

                            <div>
                                <p style={{
                                    fontSize: 'var(--text-base)',
                                    fontWeight: 700,
                                    color: 'var(--success)',
                                    marginBottom: 'var(--space-1)',
                                }}>
                                    ¡Casa creada con éxito!
                                </p>
                                <p style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)' }}>
                                    Comparte este código con tus compañeros
                                </p>
                            </div>

                            {/* Código de invitación */}
                            <div style={{
                                background: 'var(--accent-ultra-light)',
                                border: '1px solid rgba(124,58,237,0.3)',
                                borderRadius: 'var(--radius-lg)',
                                padding: 'var(--space-5) var(--space-8)',
                                width: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center'
                            }}>
                                <p style={{
                                    fontSize: 'var(--text-xs)',
                                    fontWeight: 700,
                                    textTransform: 'uppercase',
                                    letterSpacing: '0.1em',
                                    color: 'var(--text-tertiary)',
                                    marginBottom: 'var(--space-2)',
                                }}>
                                    Código de Invitación
                                </p>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <p style={{
                                        fontFamily: 'var(--font-mono)',
                                        fontSize: 'var(--text-4xl)',
                                        fontWeight: 900,
                                        letterSpacing: '0.25em',
                                        color: 'var(--accent-light)',
                                        lineHeight: 1,
                                        margin: 0,
                                        paddingLeft: '0.25em'
                                    }}>
                                        {createdHouse.inviteCode}
                                    </p>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            navigator.clipboard.writeText(createdHouse.inviteCode);
                                            setCopied(true);
                                            setTimeout(() => setCopied(false), 2000);
                                        }}
                                        style={{
                                            background: 'transparent',
                                            border: 'none',
                                            padding: '8px',
                                            cursor: 'pointer',
                                            color: copied ? 'var(--success)' : 'var(--text-secondary)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            borderRadius: 'var(--radius-md)',
                                            transition: 'color 0.15s, background 0.15s'
                                        }}
                                        onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-elevated-hover)'}
                                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                                        title="Copiar código de invitación"
                                    >
                                        {copied ? <IconCheckMini /> : <IconCopy />}
                                    </button>
                                </div>
                            </div>

                            <Button variant="primary" size="lg" full onClick={() => navigate('/')}>
                                Ir al Panel Principal →
                            </Button>
                        </div>
                    ) : (
                        /* ── FORMULARIO ── */
                        <form
                            onSubmit={handleSubmit}
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 'var(--space-4)',
                                marginTop: error ? 'var(--space-4)' : 0,
                            }}
                        >
                            <Input
                                id="house-name"
                                label="Nombre del piso / casa"
                                type="text"
                                value={formData.name}
                                onChange={handleChange('name')}
                                placeholder="Ej: Piso Calle Mayor 3B"
                                required
                            />
                            <Input
                                id="house-pic"
                                label="URL foto de la casa"
                                type="url"
                                value={formData.profilePictureUrl}
                                onChange={handleChange('profilePictureUrl')}
                                placeholder="https://... (opcional)"
                                hint="Puedes añadirla más adelante desde el panel"
                            />

                            <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
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
                                >
                                    {!loading && <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><IconHomePlus /> Fundar Casa</span>}
                                </Button>
                            </div>
                        </form>
                    )}
                </Card>

                {/* Back link */}
                {!createdHouse && (
                    <div style={{ textAlign: 'center', marginTop: 'var(--space-5)' }}>
                        <Link
                            to="/"
                            style={{ fontSize: 'var(--text-sm)', color: 'var(--text-tertiary)' }}
                        >
                            ← Volver al panel
                        </Link>
                    </div>
                )}
            </div>
        </div>
    );
}