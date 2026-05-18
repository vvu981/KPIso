/**
 * Navbar — Barra de navegación global premium
 *
 * Principio S: responsabilidad única de navegación y control de tema/sesión
 * Principio O: extensible con nuevos controles sin modificar la estructura
 * Principio L: los consumidores dependen de abstracciones (props)
 * Principio I: props mínimas y bien segregadas
 * Principio D: no depende de contextos, solo de props (inyección de dependencias)
 *
 * @param {string} username — nombre de usuario para mostrar
 * @param {() => void} onLogout — callback para logout
 * @param {boolean} isDark — si está en modo oscuro
 * @param {() => void} onToggleTheme — callback para cambiar tema
 * @param {Array} navItems — items de navegación (opcional) [{label, href, icon?}]
 */
import { useState, useContext, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../../context/authContextValue';
import api from '../../api/client';
import { Button } from '../ui/Button.jsx';
import { Input } from '../ui/Input.jsx';
import { Modal } from '../ui/Modal.jsx';
import logo from '../../assets/favicon.svg';
import { Alert } from '../ui/Alert';
import { IconSun, IconMoon, IconLogOut, IconXMark } from '../ui/Icons.jsx';



/**
 * Componente auxiliar: Avatar de usuario en navbar
 */
function UserAvatar({ username, profilePictureUrl, onClick }) {
    return (
        <button
            onClick={onClick}
            className="navbar-user"
            style={{
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '4px',
                borderRadius: 'var(--radius-md)',
                transition: 'background-color 0.2s',
            }}
            title="Editar perfil"
        >
            <div
                style={{
                    width: 32,
                    height: 32,
                    borderRadius: 'var(--radius-md)',
                    background: profilePictureUrl ? 'transparent' : 'var(--accent-ultra-light)',
                    border: profilePictureUrl ? 'none' : '1px solid rgba(124,58,237,0.25)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 'var(--text-sm)',
                    fontWeight: 'var(--font-black)',
                    color: 'var(--accent-light)',
                    flexShrink: 0,
                    overflow: 'hidden',
                }}
                aria-label={username}
            >
                {profilePictureUrl ? (
                    <img src={profilePictureUrl} alt={username} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                    username.charAt(0).toUpperCase()
                )}
            </div>
            <strong>{username}</strong>
        </button>
    );
}

/**
 * Componente auxiliar: Modal de perfil
 */
function ProfileModal({ show, onClose, currentUsername, currentEmail, currentProfilePic }) {
    const { userId, updateProfile, logoutUser } = useContext(AuthContext);
    const [username, setUsername] = useState(currentUsername || '');
    const [email, setEmail] = useState(currentEmail || '');
    const [profilePictureUrl, setProfilePictureUrl] = useState(currentProfilePic || '');
    const [currentPassword, setCurrentPassword] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [deletePassword, setDeletePassword] = useState('');

    useEffect(() => {
        if (show) {
            setUsername(currentUsername || '');
            setEmail(currentEmail || '');
            setProfilePictureUrl(currentProfilePic || '');
            setCurrentPassword('');
            setPassword('');
            setError('');
            setShowDeleteConfirm(false);
            setDeletePassword('');
        }
    }, [show, currentUsername, currentEmail, currentProfilePic]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const payload = { 
                username: username.trim() || currentUsername, 
                email: email.trim() || currentEmail, 
                profilePictureUrl: profilePictureUrl.trim() || currentProfilePic 
            };
            if (password) {
                payload.password = password;
                payload.currentPassword = currentPassword;
            }
            const res = await api.put(`/users/${userId}`, payload);
            updateProfile(res.data.username, res.data.email, res.data.profilePictureUrl);
            onClose();
        } catch (err) {
            setError(err.response?.data?.message || 'Error al actualizar el perfil.');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await api.delete(`/users/${userId}`, { data: { password: deletePassword } });
            onClose();
            if (logoutUser) logoutUser();
        } catch (err) {
            setError(err.response?.data?.message || 'Error al eliminar la cuenta.');
            setLoading(false);
        }
    };

    return (
        <Modal show={show} onClose={() => { setShowDeleteConfirm(false); setDeletePassword(''); onClose(); }} title="Editar Perfil" size="md">
            {error && <Alert type="error" message={error} />}
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)', marginTop: error ? 'var(--space-4)' : 0 }}>
                <Input
                    id="profile-username"
                    label="Nombre de usuario"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder={currentUsername}
                    disabled={loading}
                />
                <Input
                    id="profile-email"
                    label="Correo electrónico"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={currentEmail}
                    disabled={loading}
                />
                <Input
                    id="profile-pic"
                    label="URL de foto de perfil"
                    type="url"
                    value={profilePictureUrl}
                    onChange={(e) => setProfilePictureUrl(e.target.value)}
                    placeholder={currentProfilePic || "https://..."}
                    disabled={loading}
                />
                <div style={{ borderTop: '1px solid var(--border-subtle)', margin: 'var(--space-2) 0' }} />
                <Input
                    id="profile-new-password"
                    label="Nueva contraseña (opcional)"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={loading}
                />
                {password && (
                    <Input
                        id="profile-current-password"
                        label="Contraseña actual (requerida para cambiar)"
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        required={!!password}
                        disabled={loading}
                    />
                )}
                <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                    <Button variant="secondary" size="md" full type="button" onClick={onClose} disabled={loading}>
                        Cancelar
                    </Button>
                    <Button type="submit" variant="primary" size="md" full loading={loading}>
                        Guardar
                    </Button>
                </div>

                {!showDeleteConfirm ? (
                    <div style={{ marginTop: 'var(--space-6)', paddingTop: 'var(--space-4)', borderTop: '1px solid var(--border-subtle)' }}>
                        <h4 style={{ color: 'var(--danger)', fontSize: 'var(--text-sm)', marginBottom: 'var(--space-2)' }}>Zona de Peligro</h4>
                        <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', marginBottom: 'var(--space-3)' }}>
                            Al eliminar tu cuenta, perderás el acceso a todas tus viviendas de forma permanente.
                        </p>
                        <Button variant="danger" size="sm" type="button" onClick={() => setShowDeleteConfirm(true)} disabled={loading}>
                            Eliminar Cuenta
                        </Button>
                    </div>
                ) : (
                    <div style={{ marginTop: 'var(--space-6)', paddingTop: 'var(--space-4)', borderTop: '1px solid var(--danger)' }}>
                        <h4 style={{ color: 'var(--danger)', fontSize: 'var(--text-sm)', marginBottom: 'var(--space-2)' }}>Confirmar Eliminación</h4>
                        <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', marginBottom: 'var(--space-3)' }}>
                            Por favor, introduce tu contraseña para confirmar. Esta acción no se puede deshacer.
                        </p>
                        <Input
                            id="delete-password"
                            label="Contraseña actual"
                            type="password"
                            value={deletePassword}
                            onChange={(e) => setDeletePassword(e.target.value)}
                            disabled={loading}
                        />
                        <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-3)' }}>
                            <Button variant="secondary" size="sm" type="button" onClick={() => setShowDeleteConfirm(false)} disabled={loading}>
                                Cancelar
                            </Button>
                            <Button variant="danger" size="sm" type="button" onClick={handleDelete} loading={loading} disabled={!deletePassword}>
                                Confirmar y Eliminar
                            </Button>
                        </div>
                    </div>
                )}
            </form>
        </Modal>
    );
}

/**
 * Componente auxiliar: Toggle de tema
 */
export function ThemeToggle({ isDark, onToggleTheme }) {
    return (
        <button
            onClick={onToggleTheme}
            className="theme-toggle"
            aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
            title={isDark ? 'Modo claro' : 'Modo oscuro'}
            aria-pressed={isDark}
            type="button"
            style={{ background: isDark ? 'var(--accent)' : 'var(--border-default)' }}
        >
            <div
                className="theme-toggle-knob"
                style={{
                    transform: isDark ? 'translateX(20px)' : 'translateX(0)',
                }}
                aria-hidden="true"
            />
        </button>
    );
}

/**
 * Componente principal Navbar
 */
export function Navbar({
    username,
    email,
    profilePictureUrl,
    onLogout,
    isDark,
    onToggleTheme,
    navItems = [],
}) {
    const [showProfile, setShowProfile] = useState(false);

    return (
        <nav className="navbar" aria-label="Navegación principal">
            {/* Logo/Brand */}
            <Link
                to="/"
                className="navbar-brand"
                aria-label="KPIso - Ir al inicio"
                title="Ir al inicio"
                style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
                <img src={logo} alt="KPIso Logo" style={{ height: '44px', width: 'auto' }} />
                KPIso
            </Link>

            {/* Navegación items (si se proporcionan) */}
            {navItems.length > 0 && (
                <ul className="navbar-nav">
                    {navItems.map((item) => (
                        <li key={item.href} className="navbar-nav-item">
                            <Link
                                to={item.href}
                                className="navbar-nav-link"
                                aria-current={item.active ? 'page' : undefined}
                            >
                                {item.icon && <span aria-hidden="true">{item.icon}</span>}
                                {item.label}
                            </Link>
                        </li>
                    ))}
                </ul>
            )}

            {/* Controles derechos */}
            <div className="navbar-right">
                {/* Toggle Tema */}
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

                {/* Info de usuario */}
                {username && <UserAvatar username={username} profilePictureUrl={profilePictureUrl} onClick={() => setShowProfile(true)} />}

                {/* Botón de logout */}
                {onLogout && (
                    <button
                        onClick={onLogout}
                        className="btn btn-ghost btn-sm btn-icon-only"
                        title="Cerrar sesión"
                        aria-label="Cerrar sesión"
                        type="button"
                    >
                        <IconLogOut />
                    </button>
                )}
            </div>

            {showProfile && (
                <ProfileModal
                    show={showProfile}
                    onClose={() => setShowProfile(false)}
                    currentUsername={username}
                    currentEmail={email}
                    currentProfilePic={profilePictureUrl}
                />
            )}
        </nav>
    );
}
