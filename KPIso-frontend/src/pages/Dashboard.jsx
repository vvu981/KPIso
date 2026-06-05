import { useEffect, useState, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/authContextValue.js';
import api from '../api/client';
import { Button } from '../components/ui/Button.jsx';
import { Input } from '../components/ui/Input.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { Modal } from '../components/ui/Modal.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { TabBar } from '../components/ui/TabBar.jsx';
import { PageLoader } from '../components/layout/PageLoader.jsx';

// ── Constantes ──────────────────────────────────────────────────────────────
// SVG icons (Heroicons outline style)
const IconHome = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
);
const IconTrash = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="3 6 5 6 21 6"/>
        <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
        <path d="M10 11v6M14 11v6"/>
        <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
    </svg>
);
const IconPlus = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
    </svg>
);
const IconKey = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="7.5" cy="15.5" r="5.5"/>
        <path d="M21 2l-9.6 9.6"/>
        <path d="M15.5 7.5l3 3L22 7l-3-3"/>
    </svg>
);
const IconArchive = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="21 8 21 21 3 21 3 8"/>
        <rect x="1" y="3" width="22" height="5"/>
        <line x1="10" y1="12" x2="14" y2="12"/>
    </svg>
);
const IconCopy = () => (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
    </svg>
);
const IconCheckMini = () => (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--success, #10b981)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12"/>
    </svg>
);

const HOUSE_TABS = [
    { id: 'active', label: 'Casas Activas', icon: <IconHome /> },
    { id: 'deleted', label: 'Eliminadas', icon: <IconArchive /> },
];

const MODAL_INITIAL = { name: '', inviteCode: '', profilePictureUrl: '' };

/**
 * Dashboard — Panel principal de gestión de viviendas
 */
export default function Dashboard() {
    const authContext = useContext(AuthContext);
    const { userId = null, username = null } = authContext || {};
    const navigate = useNavigate();

    // Estado de datos
    const [activeHouses, setActiveHouses] = useState([]);
    const [deletedHouses, setDeletedHouses] = useState([]);
    const [activeTab, setActiveTab] = useState('active');
    const [loading, setLoading] = useState(true);

    // Estado del modal
    const [showModal, setShowModal] = useState(false);
    const [modalType, setModalType] = useState('create');
    const [formData, setFormData] = useState(MODAL_INITIAL);
    const [modalError, setModalError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    // Verificar autenticación
    useEffect(() => {
        if (!userId) navigate('/login', { replace: true });
    }, [userId, navigate]);

    /**
     * Cargar casas del usuario desde la API
     */
    const fetchHouses = async () => {
        try {
            const [activeRes, deletedRes] = await Promise.all([
                api.get(`/houses/user/${userId}`),
                api.get(`/houses/user/${userId}/deleted`),
            ]);
            setActiveHouses(activeRes.data);
            setDeletedHouses(deletedRes.data);
        } catch (err) {
            console.error('Error cargando casas:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (userId) fetchHouses();
    }, [userId]);

    /**
     * Abrir modal (crear o unirse)
     */
    const openModal = (type) => {
        setModalType(type);
        setFormData(MODAL_INITIAL);
        setModalError('');
        setShowModal(true);
    };

    /**
     * Manejar acción del modal (crear/unirse)
     */
    const handleModalAction = async (e) => {
        e.preventDefault();
        setModalError('');
        setSubmitting(true);

        try {
            if (modalType === 'create') {
                await api.post('/houses', {
                    name: formData.name,
                    creatorId: userId,
                    profilePictureUrl: formData.profilePictureUrl,
                });
            } else {
                await api.post('/houses/join', {
                    inviteCode: formData.inviteCode,
                    userId,
                });
            }
            setShowModal(false);
            fetchHouses();
        } catch (err) {
            setModalError(err.response?.data?.message || 'Error al procesar la solicitud.');
        } finally {
            setSubmitting(false);
        }
    };

    /**
     * Archivar una vivienda
     */
    const handleDeleteHouse = async (houseId) => {
        if (!window.confirm('¿Archivar esta vivienda? Solo el administrador puede restaurarla.')) {
            return;
        }

        try {
            await api.delete(`/houses/${houseId}?userId=${userId}`);
            fetchHouses();
        } catch (err) {
            alert(err.response?.data?.message || 'Solo el administrador puede archivar la casa.');
        }
    };

    // Estado de carga
    if (loading) return <PageLoader message="Cargando tu panel..." size="lg" variant="gradient" />;

    const displayedHouses = activeTab === 'active' ? activeHouses : deletedHouses;
    const isEmpty = displayedHouses.length === 0;

    return (
        <div
            style={{
                minHeight: '100vh',
                paddingTop: 'var(--space-8)',
                paddingBottom: 'var(--space-12)',
            }}
        >
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

                {/* ─────────── HEADER/HERO ─────────── */}
                <section style={{ marginBottom: 'var(--space-10)' }}>
                    <div style={{ marginBottom: 'var(--space-2)' }}>
                        <span
                            style={{
                                fontSize: 'var(--text-xs)',
                                fontWeight: 'var(--font-bold)',
                                textTransform: 'uppercase',
                                letterSpacing: '0.12em',
                                color: 'var(--accent-light)',
                                display: 'inline-block',
                            }}
                        >
                            Panel de Control
                        </span>
                    </div>
                    <h1
                        style={{
                            fontSize: 'var(--text-4xl)',
                            fontWeight: 'var(--font-black)',
                            letterSpacing: '-0.02em',
                            color: 'var(--text-primary)',
                            marginBottom: 'var(--space-3)',
                        }}
                    >
                        Hola, <span style={{ color: 'var(--accent)' }}>{username}</span>
                    </h1>
                    <p
                        style={{
                            fontSize: 'var(--text-lg)',
                            color: 'var(--text-secondary)',
                            maxWidth: '600px',
                        }}
                    >
                        Gestiona tus viviendas y colaboradores de forma segura y eficiente.
                    </p>
                </section>

                {/* ─────────── ESTADÍSTICAS ─────────── */}
                <section style={{ marginBottom: 'var(--space-10)' }}>
                    <div
                        style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                            gap: 'var(--space-5)',
                        }}
                    >
                        {[
                            {
                                label: 'Casas Activas',
                                value: activeHouses.length,
                                icon: <IconHome />,
                                color: 'var(--accent)',
                            },
                            {
                                label: 'Eliminadas',
                                value: deletedHouses.length,
                                icon: <IconArchive />,
                                color: 'var(--text-tertiary)',
                            },
                        ].map((stat) => (
                            <StatCard key={stat.label} {...stat} />
                        ))}
                    </div>
                </section>

                {/* ─────────── CONTROLES + TABS ─────────── */}
                <section
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 'var(--space-6)',
                        flexWrap: 'wrap',
                        marginBottom: 'var(--space-8)',
                    }}
                >
                    <TabBar tabs={HOUSE_TABS} activeTab={activeTab} onTabChange={setActiveTab} />

                    {activeTab === 'active' && (
                        <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
                            <Button
                                variant="primary"
                                size="md"
                                onClick={() => openModal('create')}
                            >
                                <IconPlus /> Crear Casa
                            </Button>
                            <Button
                                variant="cyan"
                                size="md"
                                onClick={() => openModal('join')}
                            >
                                <IconKey /> Unirse
                            </Button>
                        </div>
                    )}
                </section>

                {/* ─────────── GRID DE CASAS ─────────── */}
                <section>
                    {isEmpty ? (
                        <EmptyState activeTab={activeTab} onCreateClick={() => openModal('create')} onJoinClick={() => openModal('join')} />
                    ) : (
                        <div
                            style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                                gap: 'var(--space-6)',
                            }}
                        >
                            {displayedHouses.map((house) => (
                                <HouseCard
                                    key={house.id}
                                    house={house}
                                    isDeleted={activeTab === 'deleted'}
                                    onDelete={() => handleDeleteHouse(house.id)}
                                />
                            ))}
                        </div>
                    )}
                </section>
            </div>

            {/* ─────────── MODAL ─────────── */}
            <HouseModal
                show={showModal}
                type={modalType}
                onClose={() => setShowModal(false)}
                onSubmit={handleModalAction}
                formData={formData}
                onFormChange={setFormData}
                error={modalError}
                submitting={submitting}
            />
        </div>
    );
}

/**
 * StatCard — Tarjeta de estadística
 */
function StatCard({ label, value, icon, color }) {
    return (
        <Card glass padding="lg">
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                <div
                    style={{
                        width: 56,
                        height: 56,
                        borderRadius: 'var(--radius-lg)',
                        background: `${color}15`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color,
                        flexShrink: 0,
                    }}
                    aria-hidden="true"
                >
                    <span style={{ display: 'flex', transform: 'scale(1.6)', transformOrigin: 'center' }}>{icon}</span>
                </div>
                <div>
                    <div
                        style={{
                            fontSize: 'var(--text-3xl)',
                            fontWeight: 'var(--font-black)',
                            color,
                            lineHeight: 1,
                        }}
                    >
                        {value}
                    </div>
                    <div style={{ fontSize: 'var(--text-xs)', color: 'var(--text-tertiary)', marginTop: 'var(--space-1)' }}>
                        {label}
                    </div>
                </div>
            </div>
        </Card>
    );
}

/**
 * EmptyState — Estado vacío con CTA
 */
function EmptyState({ activeTab, onCreateClick, onJoinClick }) {
    const isActive = activeTab === 'active';

    return (
        <div className="empty-state">
            <div
                className="empty-state-icon"
                style={{
                    color: 'var(--border-strong)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
                {isActive ? (
                    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                        <polyline points="9 22 9 12 15 12 15 22"/>
                    </svg>
                ) : (
                    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <polyline points="21 8 21 21 3 21 3 8"/>
                        <rect x="1" y="3" width="22" height="5"/>
                        <line x1="10" y1="12" x2="14" y2="12"/>
                    </svg>
                )}
            </div>
            <h2 className="empty-state-title">
                {isActive ? 'Sin viviendas activas' : 'Sin viviendas eliminadas'}
            </h2>
            <p className="empty-state-desc" style={{ maxWidth: '400px' }}>
                {isActive
                    ? 'Crea tu primera casa o únete a una existente con un código de invitación.'
                    : 'Las viviendas que elimines aparecerán aquí.'}
            </p>
            {isActive && (
                <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-6)' }}>
                    <Button variant="primary" size="lg" onClick={onCreateClick}>
                        <IconPlus /> Crear Casa
                    </Button>
                    <Button variant="secondary" size="lg" onClick={onJoinClick}>
                        <IconKey /> Unirse con código
                    </Button>
                </div>
            )}
        </div>
    );
}

/**
 * HouseCard — Tarjeta de vivienda
 */
function HouseCard({ house, isDeleted, onDelete }) {
    const [copied, setCopied] = useState(false);
    const initial = house.name.charAt(0).toUpperCase();

    return (
        <Card interactive={true} padding="none" className="anim-fade-in">
            <div style={{ padding: 'var(--space-6)' }}>
                {/* Header: Avatar + Nombre */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)', marginBottom: 'var(--space-5)' }}>
                    <div
                        style={{
                            width: 48,
                            height: 48,
                            borderRadius: 'var(--radius-md)',
                            background: house.profilePictureUrl ? 'transparent' : 'var(--accent-ultra-light)',
                            border: house.profilePictureUrl ? 'none' : '2px solid var(--accent-light)',
                            overflow: 'hidden',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: 'var(--text-lg)',
                            fontWeight: 'var(--font-black)',
                            color: 'var(--accent-light)',
                            flexShrink: 0,
                        }}
                    >
                        {house.profilePictureUrl ? (
                            <img
                                src={house.profilePictureUrl}
                                alt={house.name}
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            />
                        ) : (
                            initial
                        )}
                    </div>
                    <div style={{ minWidth: 0 }}>
                        <h3
                            style={{
                                fontWeight: 'var(--font-bold)',
                                fontSize: 'var(--text-lg)',
                                color: 'var(--text-primary)',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                marginBottom: 'var(--space-1)',
                            }}
                        >
                            {house.name}
                        </h3>
                        <span
                            style={{
                                fontSize: 'var(--text-xs)',
                                color: 'var(--text-tertiary)',
                                fontFamily: 'var(--font-mono)',
                                fontWeight: 'var(--font-medium)',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px'
                            }}
                        >
                            {house.inviteCode}
                            <button
                                type="button"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    navigator.clipboard.writeText(house.inviteCode);
                                    setCopied(true);
                                    setTimeout(() => setCopied(false), 2000);
                                }}
                                style={{
                                    background: 'transparent',
                                    border: 'none',
                                    padding: '2px',
                                    cursor: 'pointer',
                                    color: copied ? 'var(--success)' : 'var(--text-tertiary)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    borderRadius: 'var(--radius-sm)',
                                    transition: 'color 0.15s, background 0.15s'
                                }}
                                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-elevated-hover)'}
                                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                                title="Copiar código de invitación"
                            >
                                {copied ? <IconCheckMini /> : <IconCopy />}
                            </button>
                        </span>
                    </div>
                </div>

                {/* Separador */}
                <div style={{ borderTop: '1px solid var(--border-subtle)', marginBottom: 'var(--space-4)' }} />

                {/* Footer: Estado + Acciones */}
                {isDeleted ? (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 'var(--space-3)' }}>
                        <Link
                            to={`/house/${house.id}`}
                            style={{
                                flex: 1,
                                textDecoration: 'none',
                            }}
                        >
                            <Button variant="secondary" size="sm" full>
                                Entrar (Solo Lectura)
                            </Button>
                        </Link>
                        <Badge variant="neutral" size="sm">
                            Eliminada
                        </Badge>
                    </div>
                ) : (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 'var(--space-3)' }}>
                        <Link
                            to={`/house/${house.id}`}
                            style={{
                                flex: 1,
                                textDecoration: 'none',
                            }}
                        >
                            <Button variant="primary" size="sm" full>
                                Entrar
                            </Button>
                        </Link>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                                e.stopPropagation();
                                onDelete();
                            }}
                            title="Eliminar vivienda"
                            aria-label="Eliminar vivienda"
                        >
                            <IconTrash />
                        </Button>
                    </div>
                )}
            </div>
        </Card>
    );
}

/**
 * HouseModal — Modal para crear o unirse a vivienda
 */
function HouseModal({
                        show,
                        type,
                        onClose,
                        onSubmit,
                        formData,
                        onFormChange,
                        error,
                        submitting,
                    }) {
    const isCreate = type === 'create';

    return (
        <Modal
            show={show}
            onClose={onClose}
            title={isCreate ? 'Crear Nueva Casa' : 'Unirse con Código'}
            size="sm"
        >
            {error && (
                <Alert
                    type="error"
                    message={error}
                    dismissible={true}
                    onClose={() => {
                        // Limpiar error es responsabilidad del padre
                    }}
                />
            )}

            <form
                onSubmit={onSubmit}
                style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 'var(--space-5)',
                    marginTop: error ? 'var(--space-4)' : 0,
                }}
            >
                {isCreate ? (
                    <>
                        <Input
                            id="house-name"
                            label="Nombre de la casa"
                            type="text"
                            value={formData.name}
                            onChange={(e) => onFormChange({ ...formData, name: e.target.value })}
                            placeholder="Ej: Piso Centro, Apartamento Playa"
                            required
                            disabled={submitting}
                        />
                        <Input
                            id="house-picture"
                            label="URL de foto (opcional)"
                            type="url"
                            value={formData.profilePictureUrl}
                            onChange={(e) => onFormChange({ ...formData, profilePictureUrl: e.target.value })}
                            placeholder="https://..."
                            disabled={submitting}
                        />
                    </>
                ) : (
                    <Input
                        id="invite-code"
                        label="Código de invitación"
                        type="text"
                        value={formData.inviteCode}
                        onChange={(e) => onFormChange({ ...formData, inviteCode: e.target.value.toUpperCase() })}
                        placeholder="AX93LD"
                        maxLength={6}
                        required
                        disabled={submitting}
                        mono={true}
                    />
                )}

                <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                    <Button
                        variant="secondary"
                        size="md"
                        full
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                    >
                        Cancelar
                    </Button>
                    <Button
                        type="submit"
                        variant="primary"
                        size="md"
                        full
                        loading={submitting}
                    >
                        {submitting ? 'Procesando...' : 'Confirmar'}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}