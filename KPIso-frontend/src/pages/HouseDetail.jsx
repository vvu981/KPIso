import { useEffect, useState, useContext } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/authContextValue.js';
import api from '../api/client';
import { Button } from '../components/ui/Button.jsx';
import { Card } from '../components/ui/Card.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { Input } from '../components/ui/Input.jsx';
import { PageLoader } from '../components/layout/PageLoader.jsx';
import {
    IconClipboardDocumentList, IconBanknotes, IconMagnifyingGlass,
    IconCalendar, IconListBullet, IconCheck, IconXMark,
    IconPlus, IconReceiptRefund
} from '../components/ui/Icons.jsx';

// ── SVG Icons (Heroicons outline) ───────────────────────────────────────
const IconPencil = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
    </svg>
);
const IconTrash = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="3 6 5 6 21 6"/>
        <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
        <path d="M10 11v6M14 11v6"/>
        <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
    </svg>
);
const IconClipboard = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
        <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
    </svg>
);
const IconCurrencyEuro = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="10"/>
        <text x="7" y="17" fontFamily="serif" fontSize="14" fill="currentColor" stroke="none">€</text>
    </svg>
);
const IconShield = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
    </svg>
);
const IconUsers = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
);
const IconStar = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
    </svg>
);
const IconX = () => (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
);
const IconChevronLeft = () => (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="15 18 9 12 15 6"/>
    </svg>
);

/**
 * HouseDetail — Página de detalle y gestión de vivienda
 */
export default function HouseDetail() {
    const { houseId } = useParams();
    const authContext = useContext(AuthContext);
    const { userId: currentUserId = null, token = null, username: currentUsername = null } = authContext || {};
    const navigate = useNavigate();

    // Estado global de datos
    const [house, setHouse] = useState(null);
    const [tasks, setTasks] = useState([]);
    const [expenses, setExpenses] = useState([]);
    const [settlements, setSettlements] = useState([]);
    const [activityLogs, setActivityLogs] = useState([]);
    const [memberStatuses, setMemberStatuses] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Estado de UI
    const [activeTab, setActiveTab] = useState('tasks');
    const [taskViewMode, setTaskViewMode] = useState('list');
    const [sidebarView, setSidebarView] = useState('money');
    const [currentMonth, setCurrentMonth] = useState(new Date().getMonth());
    const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
    const [selectedTask, setSelectedTask] = useState(null);
    const [selectedUserFilter, setSelectedUserFilter] = useState(''); // NUEVO: Estado del filtro de conviviente

    // Estado de formularios
    const [showTaskForm, setShowTaskForm] = useState(false);
    const [editingTaskId, setEditingTaskId] = useState(null);
    const [taskForm, setTaskForm] = useState({
        title: '',
        description: '',
        points: 5,
        rotationType: 'WEEKLY',
        assignedToId: '',
        participantIds: [],
        specificDays: [],
        occurrencesToProject: 4,
        startDate: '',
    });

    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [editingExpenseId, setEditingExpenseId] = useState(null);
    const [expenseForm, setExpenseForm] = useState({
        title: '',
        amount: '',
        paidById: currentUserId,
        participantIds: [],
    });

    const [showEditHouseModal, setShowEditHouseModal] = useState(false);
    const [editHouseForm, setEditHouseForm] = useState({ name: '', profilePictureUrl: '' });

    // Modales de Perfil de Usuario
    const [showEditProfileModal, setShowEditProfileModal] = useState(false);
    const [profileFormData, setProfileFormData] = useState({ username: '', profilePictureUrl: '' });

    // Diálogos personalizados
    const [customDialog, setCustomDialog] = useState({
        show: false,
        title: 'Aviso',
        message: '',
        type: 'alert',
        onConfirm: null,
    });

    // Verificar autenticación
    useEffect(() => {
        if (!currentUserId) {
            navigate('/login', { replace: true });
        }
    }, [currentUserId, navigate]);

    /**
     * Cargar todos los datos de la vivienda
     */
    const fetchData = async () => {
        try {
            const [houseRes, tasksRes, expensesRes, settlementRes, historyRes, statusRes] = await Promise.all([
                api.get(`/houses/${houseId}`),
                api.get(`/tasks/house/${houseId}`),
                api.get(`/expenses/house/${houseId}`),
                api.get(`/expenses/house/${houseId}/settlement`),
                api.get(`/activities/house/${houseId}`),
                api.get(`/expenses/house/${houseId}/statuses`),
            ]);

            setHouse(houseRes.data);
            setTasks(tasksRes.data);
            setExpenses(expensesRes.data);
            setSettlements(settlementRes.data);
            setActivityLogs(historyRes.data);
            setMemberStatuses(statusRes.data);
        } catch (err) {
            setError('Error al cargar la vivienda. Intenta más tarde.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (houseId && currentUserId) {
            fetchData();
        }
    }, [houseId, currentUserId]);

    // Helpers de diálogo
    const showAlert = (message, title = 'Aviso') => {
        setCustomDialog({ show: true, title, message, type: 'alert', onConfirm: null });
    };

    const showConfirm = (message, onConfirm, title = 'Confirmar') => {
        setCustomDialog({ show: true, title, message, type: 'confirm', onConfirm });
    };

    // Cambiar preferencia de color de un miembro
    const handleColorChange = async (newColor) => {
        try {
            await api.patch(`/houses/${houseId}/members/color?userId=${currentUserId}&color=${encodeURIComponent(newColor)}`);
            fetchData();
        } catch (err) {
            showAlert('No se pudo guardar la preferencia de color.');
        }
    };

    /**
     * Guardar cambios del perfil de usuario y sincronizar el contexto global
     */
    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        try {
            await api.put(`/users/${currentUserId}`, {
                username: profileFormData.username,
                profilePictureUrl: profileFormData.profilePictureUrl,
            });

            if (authContext && authContext.loginUser) {
                authContext.loginUser(token, currentUserId, profileFormData.username);
            }

            setShowEditProfileModal(false);
            fetchData();
        } catch (err) {
            showAlert('No se pudieron guardar los cambios de tu perfil de usuario.');
        }
    };

    // Handlers para tareas
    const handleCreateOrUpdateTask = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                title: taskForm.title,
                description: taskForm.description,
                points: parseInt(taskForm.points),
                houseId,
                rotationType: taskForm.rotationType,
                occurrencesToProject: parseInt(taskForm.occurrencesToProject),
                startDate: editingTaskId
                    ? taskForm.startDate
                        ? `${taskForm.startDate}T12:00:00`
                        : null
                    : new Date().toISOString().split('.')[0],
            };

            if (taskForm.rotationType === 'FIXED') {
                payload.assignedToId = taskForm.assignedToId || currentUserId;
            } else {
                payload.participantIds = taskForm.participantIds;
                if (taskForm.rotationType === 'WEEKLY') {
                    payload.specificDays = taskForm.specificDays;
                }
            }

            if (editingTaskId) {
                await api.put(`/tasks/${editingTaskId}?userId=${currentUserId}`, payload);
                setEditingTaskId(null);
            } else {
                await api.post('/tasks', payload);
            }

            setTaskForm({
                title: '',
                description: '',
                points: 5,
                rotationType: 'WEEKLY',
                assignedToId: '',
                participantIds: [],
                specificDays: [],
                occurrencesToProject: 4,
                startDate: '',
            });
            setShowTaskForm(false);
            fetchData();
        } catch (err) {
            showAlert(err.response?.data?.message || 'Error al procesar la tarea.');
        }
    };

    const handleToggleTaskStatus = async (taskId, currentStatus) => {
        try {
            const newStatus = currentStatus === 'PENDING' ? 'COMPLETED' : 'PENDING';
            await api.patch(`/tasks/${taskId}/status?status=${newStatus}&userId=${currentUserId}`);
            fetchData();
        } catch (err) {
            showAlert('No se pudo actualizar el estado de la tarea.');
        }
    };

    const handleDragStart = (e, taskId) => {
        e.dataTransfer.setData('text/plain', taskId);
    };

    const handleDropTask = async (e, targetDateStr) => {
        e.preventDefault();
        const taskId = e.dataTransfer.getData('text/plain');
        if (!taskId) return;
        try {
            const newDueDate = new Date(targetDateStr);
            newDueDate.setHours(12, 0, 0, 0);
            const isoStr = newDueDate.toISOString().split('.')[0];
            await api.patch(`/tasks/${taskId}/due-date?dueDate=${isoStr}&userId=${currentUserId}`);
            fetchData();
        } catch (err) {
            showAlert('No se pudo reubicar la tarea.');
        }
    };

    const handleDeleteTask = async (taskId) => {
        showConfirm(
            '¿Confirmas la eliminación definitiva de esta tarea?',
            async () => {
                try {
                    await api.delete(`/tasks/${taskId}?userId=${currentUserId}`);
                    setSelectedTask(null);
                    fetchData();
                } catch (err) {
                    showAlert('No se pudo eliminar la tarea.');
                }
            },
            'Eliminar Tarea'
        );
    };

    const startEditTask = (task) => {
        setSelectedTask(null);
        setEditingTaskId(task.id);
        const dateIsoBase = task.dueDate ? task.dueDate.split('T')[0] : '';
        setTaskForm({
            title: task.title,
            description: task.description || '',
            points: task.points,
            rotationType: task.rotationType,
            assignedToId: task.assignedTo?.id || '',
            participantIds: [],
            specificDays: [],
            occurrencesToProject: 1,
            startDate: dateIsoBase,
        });
        setShowTaskForm(true);
    };

    // Handlers para gastos
    const handleCreateOrUpdateExpense = async (e) => {
        e.preventDefault();
        if (expenseForm.participantIds.length === 0) {
            showAlert('Debes seleccionar al menos a un beneficiario.');
            return;
        }

        try {
            const payload = {
                title: expenseForm.title,
                amount: parseFloat(expenseForm.amount),
                houseId,
                paidById: expenseForm.paidById,
                participantIds: expenseForm.participantIds,
            };

            if (editingExpenseId) {
                await api.put(`/expenses/${editingExpenseId}?userId=${currentUserId}`, payload);
                setEditingExpenseId(null);
            } else {
                await api.post('/expenses', payload);
            }

            setExpenseForm({
                title: '',
                amount: '',
                paidById: currentUserId,
                participantIds: [],
            });
            setShowExpenseForm(false);
            fetchData();
        } catch (err) {
            showAlert('Error al procesar el gasto.');
        }
    };

    const handleRegisterIndividualPayment = async (debtorId, debtorUsername, creditorId, creditorUsername, amount) => {
        showConfirm(
            `¿Confirmas que ${debtorUsername} ha pagado ${amount.toFixed(2)}€ a ${creditorUsername}?`,
            async () => {
                try {
                    await api.post('/expenses', {
                        title: `Liquidación: ${debtorUsername} ➔ ${creditorUsername}`,
                        amount: parseFloat(amount),
                        houseId,
                        paidById: debtorId,
                        participantIds: [creditorId]
                    });
                    fetchData();
                } catch (err) {
                    showAlert('Error al registrar el pago de la liquidación.');
                }
            },
            'Registrar Pago'
        );
    };

    // Handlers para vivienda
    const handleUpdateHouse = async (e) => {
        e.preventDefault();
        try {
            await api.put(`/houses/${houseId}?userId=${currentUserId}`, {
                name: editHouseForm.name,
                creatorId: currentUserId,
                profilePictureUrl: editHouseForm.profilePictureUrl,
            });
            setShowEditHouseModal(false);
            fetchData();
        } catch (err) {
            showAlert('Error al actualizar la configuración.');
        }
    };

    const handleDeleteHouse = async () => {
        showConfirm(
            '¿Confirmas la eliminación de esta vivienda? Se moverá al archivo.',
            async () => {
                try {
                    await api.delete(`/houses/${houseId}?userId=${currentUserId}`);
                    window.location.href = '/';
                } catch (err) {
                    showAlert(err.response?.data || 'No se pudo eliminar la vivienda.');
                }
            },
            'Eliminar Vivienda'
        );
    };

    const handleRemoveMember = async (targetUserId, targetUsername) => {
        showConfirm(
            `¿Expulsar a ${targetUsername} de esta casa?`,
            async () => {
                try {
                    await api.delete(`/houses/${houseId}/members/${targetUserId}?userId=${currentUserId}`);
                    fetchData();
                } catch (err) {
                    showAlert(err.response?.data || 'No se pudo expulsar al miembro.');
                }
            },
            'Expulsar Miembro'
        );
    };

    // Loading
    if (loading) {
        return <PageLoader message="Cargando vivienda..." size="lg" />;
    }

    if (!house) {
        return (
            <div style={{ padding: 'var(--space-8)', textAlign: 'center' }}>
                <Alert
                    type="error"
                    message="No se pudo cargar la vivienda. Intenta nuevamente."
                />
                <Link to="/" style={{ marginTop: 'var(--space-4)' }}>
                    <Button variant="primary">Volver al Panel</Button>
                </Link>
            </div>
        );
    }

    const selfMembership = house.members.find((m) => m.userId === currentUserId);
    const selfIsAdmin = selfMembership?.role === 'ADMIN';

    return (
        <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-base)' }}>
            {/* Header Principal */}
            <HouseDetailHeader
                house={house}
                activeTab={activeTab}
                onTabChange={setActiveTab}
                onEditClick={() => {
                    setEditHouseForm({
                        name: house.name,
                        profilePictureUrl: house.profilePictureUrl || '',
                    });
                    setShowEditHouseModal(true);
                }}
                onDeleteClick={handleDeleteHouse}
                isAdmin={selfIsAdmin}
            />

            {/* Contenido Principal */}
            <main
                style={{
                    maxWidth: '1200px',
                    margin: '0 auto',
                    padding: 'var(--space-8) var(--space-4)',
                    display: 'grid',
                    gridTemplateColumns: '1fr 2fr',
                    gap: 'var(--space-6)',
                }}
            >
                {/* Sidebar */}
                <aside>
                    <HouseSidebar
                        members={house.members}
                        memberStatuses={memberStatuses}
                        sidebarView={sidebarView}
                        onSidebarViewChange={setSidebarView}
                        onRemoveMember={handleRemoveMember}
                        currentUserId={currentUserId}
                        isAdmin={selfIsAdmin}
                        onColorChange={handleColorChange}
                        onEditProfile={() => {
                            setProfileFormData({
                                username: currentUsername || '',
                                profilePictureUrl: '',
                            });
                            setShowEditProfileModal(true);
                        }}
                    />
                </aside>

                {/* Contenido según Tab */}
                <section>
                    {activeTab === 'tasks' && (
                        <TasksSection
                            tasks={tasks}
                            house={house}
                            showTaskForm={showTaskForm}
                            editingTaskId={editingTaskId}
                            taskForm={taskForm}
                            onTaskFormChange={setTaskForm}
                            onSubmitTask={handleCreateOrUpdateTask}
                            onCancelTask={() => setShowTaskForm(false)}
                            onAddTask={() => setShowTaskForm(true)}
                            onEditTask={startEditTask}
                            onDeleteTask={handleDeleteTask}
                            onToggleStatus={handleToggleTaskStatus}
                            onDragStart={handleDragStart}
                            onDropTask={handleDropTask}
                            selectedTask={selectedTask}
                            onSelectTask={setSelectedTask}
                            taskViewMode={taskViewMode}
                            onTaskViewModeChange={setTaskViewMode}
                            currentMonth={currentMonth}
                            currentYear={currentYear}
                            onPrevMonth={() =>
                                currentMonth === 0
                                    ? (setCurrentMonth(11), setCurrentYear(currentYear - 1))
                                    : setCurrentMonth(currentMonth - 1)
                            }
                            onNextMonth={() =>
                                currentMonth === 11
                                    ? (setCurrentMonth(0), setCurrentYear(currentYear + 1))
                                    : setCurrentMonth(currentMonth + 1)
                            }
                            selectedUserFilter={selectedUserFilter}
                            onUserFilterChange={setSelectedUserFilter}
                        />
                    )}

                    {activeTab === 'expenses' && (
                        <ExpensesSection
                            expenses={expenses}
                            settlements={settlements}
                            memberStatuses={memberStatuses}
                            house={house}
                            showExpenseForm={showExpenseForm}
                            expenseForm={expenseForm}
                            onExpenseFormChange={setExpenses}
                            onAddExpense={() => setShowExpenseForm(true)}
                            onSubmitExpense={handleCreateOrUpdateExpense}
                            onCancelExpense={() => setShowExpenseForm(false)}
                            onSettlePayment={handleRegisterIndividualPayment}
                            setExpenseForm={setExpenseForm}
                            currentUserId={currentUserId}
                        />
                    )}

                    {activeTab === 'history' && (
                        <HistorySection activityLogs={activityLogs} />
                    )}
                </section>
            </main>

            {/* Modales y Diálogos */}
            {showEditHouseModal && (
                <EditHouseModal
                    house={house}
                    form={editHouseForm}
                    onFormChange={setEditHouseForm}
                    onSubmit={handleUpdateHouse}
                    onClose={() => setShowEditHouseModal(false)}
                />
            )}

            {showEditProfileModal && (
                <EditProfileModal
                    form={profileFormData}
                    onFormChange={setProfileFormData}
                    onSubmit={handleUpdateProfile}
                    onClose={() => setShowEditProfileModal(false)}
                />
            )}

            {customDialog.show && (
                <CustomDialog
                    title={customDialog.title}
                    message={customDialog.message}
                    type={customDialog.type}
                    onClose={() => setCustomDialog({ ...customDialog, show: false })}
                    onConfirm={() => {
                        customDialog.onConfirm?.();
                        setCustomDialog({ ...customDialog, show: false });
                    }}
                />
            )}

            {selectedTask && (
                <TaskDetailModal
                    task={selectedTask}
                    onClose={() => setSelectedTask(null)}
                    onEdit={() => startEditTask(selectedTask)}
                    onDelete={() => handleDeleteTask(selectedTask.id)}
                />
            )}
        </div>
    );
}

/**
 * HouseDetailHeader — Encabezado con navegación por tabs
 */
function HouseDetailHeader({ house, activeTab, onTabChange, onEditClick, onDeleteClick, isAdmin }) {
    return (
        <header
            style={{
                backgroundColor: 'var(--bg-surface)',
                borderBottom: '1px solid var(--border-default)',
                padding: 'var(--space-6) var(--space-4)',
                position: 'sticky',
                top: 'var(--navbar-height)',
                zIndex: 100,
            }}
        >
            <div
                style={{
                    maxWidth: '1200px',
                    margin: '0 auto',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 'var(--space-6)',
                    flexWrap: 'wrap',
                }}
            >
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                    <Link to="/" title="Volver al panel" style={{ textDecoration: 'none' }}>
                        <Button variant="ghost" size="sm">
                            ← Atrás
                        </Button>
                    </Link>

                    <div
                        style={{
                            width: 48,
                            height: 48,
                            borderRadius: 'var(--radius-lg)',
                            overflow: 'hidden',
                            backgroundColor: 'var(--accent-ultra-light)',
                            border: '2px solid var(--accent-light)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: 'var(--text-xl)',
                            fontWeight: 'var(--font-black)',
                            color: 'var(--accent-light)',
                        }}
                    >
                        {house.profilePictureUrl ? (
                            <img
                                src={house.profilePictureUrl}
                                alt={house.name}
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            />
                        ) : (
                            house.name.charAt(0).toUpperCase()
                        )}
                    </div>

                    <div>
                        <h1
                            style={{
                                fontSize: 'var(--text-2xl)',
                                fontWeight: 'var(--font-bold)',
                                color: 'var(--text-primary)',
                                marginBottom: 'var(--space-1)',
                            }}
                        >
                            {house.name}
                        </h1>
                        <p
                            style={{
                                fontSize: 'var(--text-xs)',
                                color: 'var(--text-tertiary)',
                                fontFamily: 'var(--font-mono)',
                            }}
                        >
                            Código: {house.inviteCode}
                        </p>
                    </div>
                </div>

                <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                    <Button variant="secondary" size="sm" onClick={onEditClick} title="Editar vivienda">
                        <IconPencil />
                    </Button>
                    {isAdmin && (
                        <Button
                            variant="danger"
                            size="sm"
                            onClick={onDeleteClick}
                            title="Eliminar vivienda"
                        >
                            <IconTrash />
                        </Button>
                    )}
                </div>
            </div>

            <div
                style={{
                    display: 'flex',
                    justifyContent: 'center',
                    gap: 'var(--space-2)',
                    marginTop: 'var(--space-4)',
                    borderTop: '1px solid var(--border-subtle)',
                    paddingTop: 'var(--space-4)',
                }}
            >
                {['tasks', 'expenses', 'history'].map((tab) => (
                    <button
                        key={tab}
                        onClick={() => onTabChange(tab)}
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 'var(--space-2)',
                            padding: 'var(--space-2) var(--space-4)',
                            borderRadius: 'var(--radius-md)',
                            backgroundColor: activeTab === tab ? 'var(--accent)' : 'transparent',
                            color: activeTab === tab ? '#fff' : 'var(--text-secondary)',
                            border: 'none',
                            cursor: 'pointer',
                            fontSize: 'var(--text-sm)',
                            fontWeight: 'var(--font-bold)',
                            transition: 'all 200ms var(--timing-smooth)',
                        }}
                    >
                        {tab === 'tasks' && <><IconClipboard /> Deberes</>}
                        {tab === 'expenses' && <><IconCurrencyEuro /> Gastos</>}
                        {tab === 'history' && <><IconShield /> Transparencia</>}
                    </button>
                ))}
            </div>
        </header>
    );
}

/**
 * HouseSidebar — Panel lateral con miembros e información
 */
function HouseSidebar({
                          members,
                          memberStatuses,
                          sidebarView,
                          onSidebarViewChange,
                          onRemoveMember,
                          currentUserId,
                          isAdmin,
                          onColorChange,
                          onEditProfile,
                      }) {
    return (
        <Card padding="lg" glass>
            <h2 style={{ fontSize: 'var(--text-sm)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)', marginBottom: 'var(--space-3)', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <IconUsers /> Convivientes
            </h2>

            <div style={{ display: 'flex', gap: 'var(--space-2)', marginBottom: 'var(--space-4)' }}>
                {['money', 'points'].map((view) => (
                    <button
                        key={view}
                        onClick={() => onSidebarViewChange(view)}
                        style={{
                            flex: 1,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 'var(--space-1)',
                            padding: 'var(--space-2)',
                            borderRadius: 'var(--radius-md)',
                            backgroundColor: sidebarView === view ? 'var(--bg-elevated)' : 'transparent',
                            border: '1px solid var(--border-subtle)',
                            fontSize: 'var(--text-xs)',
                            fontWeight: 'var(--font-bold)',
                            cursor: 'pointer',
                            transition: 'all 200ms var(--timing-smooth)',
                        }}
                    >
                        {view === 'money' ? <><IconCurrencyEuro /> Balance</> : <><IconStar /> Puntos</>}
                    </button>
                ))}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                {members.map((member) => {
                    const status = memberStatuses[member.userId] || { balance: 0, color: '#6366f1', points: 0 };
                    const value = sidebarView === 'money' ? `${status.balance.toFixed(2)}€` : `${status.points} pts`;

                    return (
                        <div
                            key={member.userId}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                padding: 'var(--space-3)',
                                borderRadius: 'var(--radius-lg)',
                                backgroundColor: 'var(--bg-elevated)',
                                border: '1px solid var(--border-subtle)',
                            }}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                                <div
                                    style={{
                                        width: 12,
                                        height: 12,
                                        borderRadius: '50%',
                                        backgroundColor: status.color,
                                        boxShadow: `0 0 8px ${status.color}40`,
                                    }}
                                />
                                <span style={{ fontSize: 'var(--text-sm)', fontWeight: 'var(--font-bold)' }}>
                                    {member.username}
                                </span>
                                {member.role === 'ADMIN' && (
                                    <Badge variant="primary" size="xs">
                                        Admin
                                    </Badge>
                                )}
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                                <span style={{ fontSize: 'var(--text-sm)', fontWeight: 'var(--font-bold)', color: status.balance >= 0 || sidebarView === 'points' ? 'var(--success)' : 'var(--danger)' }}>
                                    {status.balance > 0 && sidebarView === 'money' ? '+' : ''}{value}
                                </span>
                                {member.userId === currentUserId && (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-1)' }}>
                                        <input
                                            type="color"
                                            value={status.color}
                                            onChange={(e) => onColorChange(e.target.value)}
                                            style={{ width: 18, height: 18, border: 'none', borderRadius: '4px', cursor: 'pointer', backgroundColor: 'transparent' }}
                                        />
                                        <button
                                            onClick={onEditProfile}
                                            title="Editar mi perfil"
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '2px', display: 'flex', color: 'var(--text-secondary)' }}
                                        >
                                            <IconPencil />
                                        </button>
                                    </div>
                                )}
                                {isAdmin && member.userId !== currentUserId && (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => onRemoveMember(member.userId, member.username)}
                                        title="Expulsar"
                                        style={{ marginLeft: 'var(--space-1)', padding: 0 }}
                                    >
                                        <IconX />
                                    </Button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </Card>
    );
}

/**
 * TasksSection — Sección de gestión de deberes/tareas
 */
function TasksSection({
                          tasks,
                          house,
                          showTaskForm,
                          editingTaskId,
                          taskForm,
                          onTaskFormChange,
                          onSubmitTask,
                          onCancelTask,
                          onAddTask,
                          onEditTask,
                          onDeleteTask,
                          onToggleStatus,
                          onDragStart,
                          onDropTask,
                          selectedTask,
                          onSelectTask,
                          taskViewMode,
                          onTaskViewModeChange,
                          currentMonth,
                          currentYear,
                          onPrevMonth,
                          onNextMonth,
                          selectedUserFilter, // NUEVO
                          onUserFilterChange, // NUEVO
                      }) {
    const now = new Date();

    const isTaskInCurrentPeriod = (task) => {
        if (task.status === 'PENDING' && task.dueDate && new Date(task.dueDate) < now) {
            return true;
        }
        if (!task.dueDate) return true;
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const taskDay = new Date(new Date(task.dueDate).getFullYear(), new Date(task.dueDate).getMonth(), new Date(task.dueDate).getDate());

        if (task.rotationType === 'DAILY') return today.getTime() === taskDay.getTime();
        if (task.rotationType === 'WEEKLY') {
            const currentDay = now.getDay();
            const monday = new Date(today);
            monday.setDate(today.getDate() + (currentDay === 0 ? -6 : 1 - currentDay));
            const sunday = new Date(monday);
            sunday.setDate(monday.getDate() + 6);
            return taskDay >= monday && taskDay <= sunday;
        }
        if (task.rotationType === 'MONTHLY') return currentYear === new Date(task.dueDate).getFullYear() && currentMonth === new Date(task.dueDate).getMonth();
        return true;
    };

    const generateCalendarDays = () => {
        const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
        const lastDayOfMonth = new Date(currentYear, currentMonth + 1, 0);
        const dayOfWeek = firstDayOfMonth.getDay();
        const paddingSlots = (dayOfWeek + 6) % 7;

        const days = [];
        for (let i = 0; i < paddingSlots; i++) days.push(null);
        for (let d = 1; d <= lastDayOfMonth.getDate(); d++) days.push(new Date(currentYear, currentMonth, d));
        return days;
    };

    return (
        <Card padding="lg">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
                <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <IconClipboardDocumentList /> Deberes & Tareas
                    </span>
                </h2>
                {!showTaskForm && (
                    <Button variant="primary" size="sm" onClick={onAddTask}>
                        ＋ Nueva Tarea
                    </Button>
                )}
            </div>

            {showTaskForm && (
                <TaskForm
                    form={taskForm}
                    onFormChange={onTaskFormChange}
                    onSubmit={onSubmitTask}
                    onCancel={onCancelTask}
                    isEditing={!!editingTaskId}
                    house={house}
                />
            )}

            {/* MODIFICADO: Barra de controles unificada que aloja el switch de vista y el nuevo filtro de convivientes */}
            {!showTaskForm && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 'var(--space-4)', marginBottom: 'var(--space-5)', flexWrap: 'wrap' }}>
                    <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                        {['list', 'calendar'].map((mode) => (
                            <Button
                                key={mode}
                                variant={taskViewMode === mode ? 'primary' : 'secondary'}
                                size="sm"
                                onClick={() => onTaskViewModeChange(mode)}
                            >
                                {mode === 'list' ? <><IconListBullet /> Lista</> : <><IconCalendar /> Calendario</>}
                            </Button>
                        ))}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                        <label htmlFor="user-task-filter" style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>
                            Asignado a:
                        </label>
                        <select
                            id="user-task-filter"
                            value={selectedUserFilter}
                            onChange={(e) => onUserFilterChange(e.target.value)}
                            style={{
                                padding: 'var(--space-2)',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid var(--border-default)',
                                fontSize: 'var(--text-xs)',
                                backgroundColor: 'var(--bg-base)',
                                color: 'var(--text-primary)',
                                cursor: 'pointer'
                            }}
                        >
                            <option value="">-- Todos los convivientes --</option>
                            {house.members.map(m => (
                                <option key={m.userId} value={m.userId}>{m.username}</option>
                            ))}
                        </select>
                    </div>
                </div>
            )}

            {tasks.length === 0 && !showTaskForm ? (
                <div style={{ textAlign: 'center', padding: 'var(--space-8)' }}>
                    <p style={{ color: 'var(--text-tertiary)' }}>
                        No hay tareas todavía. ¡Crea la primera!
                    </p>
                </div>
            ) : (
                !showTaskForm && (
                    taskViewMode === 'list' ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                            {/* MODIFICADO: Aplicación cruzada del filtro por usuario en la lista de deberes */}
                            {tasks
                                .filter(isTaskInCurrentPeriod)
                                .filter(task => !selectedUserFilter || task.assignedTo?.id === selectedUserFilter)
                                .map((task) => {
                                    const isExpired = task.status === 'PENDING' && task.dueDate && new Date(task.dueDate) < now;
                                    return (
                                        <TaskListItem
                                            key={task.id}
                                            task={task}
                                            isExpired={isExpired}
                                            onSelect={() => onSelectTask(task)}
                                            onEdit={() => onEditTask(task)}
                                            onDelete={() => onDeleteTask(task.id)}
                                            onToggleStatus={() => onToggleStatus(task.id, task.status)}
                                        />
                                    );
                                })
                            }
                        </div>
                    ) : (
                        <div style={{ userSelect: 'none' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)', backgroundColor: 'var(--bg-elevated)', padding: 'var(--space-2)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--border-subtle)' }}>
                                <Button variant="secondary" size="sm" onClick={onPrevMonth}>◀</Button>
                                <h3 style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-black)', textTransform: 'uppercase', color: 'var(--text-secondary)' }}>
                                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <IconCalendar /> {new Date(currentYear, currentMonth).toLocaleString('es-ES', { month: 'long', year: 'numeric' })}
                                    </span>
                                </h3>
                                <Button variant="secondary" size="sm" onClick={onNextMonth}>▶</Button>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px', backgroundColor: 'var(--border-subtle)', padding: '4px', borderRadius: 'var(--radius-lg)' }}>
                                {['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map(d => (
                                    <span key={d} style={{ fontSize: '10px', fontWeight: 'bold', textAlign: 'center', color: 'var(--text-tertiary)', padding: '4px 0' }}>{d}</span>
                                ))}
                                {generateCalendarDays().map((dayDate, idx) => {
                                    if (dayDate === null) return <div key={`empty-${idx}`} style={{ minHeight: '80px', backgroundColor: 'transparent', opacity: 0.2 }}></div>;
                                    const dateStr = dayDate.toDateString();

                                    {/* MODIFICADO: Aplicación cruzada del filtro por usuario dentro de las celdas del calendario mensual */}
                                    const dayTasks = tasks
                                        .filter(t => t.dueDate && new Date(t.dueDate).toDateString() === dateStr)
                                        .filter(t => !selectedUserFilter || t.assignedTo?.id === selectedUserFilter);

                                    return (
                                        <div
                                            key={dateStr}
                                            onDragOver={e => e.preventDefault()}
                                            onDrop={e => onDropTask(e, dayDate.toISOString())}
                                            style={{ minHeight: '80px', backgroundColor: 'var(--bg-base)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-md)', p: '2px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
                                        >
                                            <span style={{ fontSize: '9px', fontWeight: 'bold', color: 'var(--text-tertiary)', padding: '2px' }}>{dayDate.getDate()}</span>
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', marginTop: '2px' }}>
                                                {dayTasks.map(task => {
                                                    const isExpired = task.status === 'PENDING' && task.dueDate && new Date(task.dueDate) < now;
                                                    return (
                                                        <div
                                                            key={task.id}
                                                            draggable={task.status === 'PENDING'}
                                                            onDragStart={e => onDragStart(e, task.id)}
                                                            onClick={(e) => { e.stopPropagation(); onSelectTask(task); }}
                                                            style={{
                                                                fontSize: '9px',
                                                                fontWeight: 'bold',
                                                                padding: '2px 4px',
                                                                borderRadius: 'var(--radius-sm)',
                                                                textOverflow: 'ellipsis',
                                                                overflow: 'hidden',
                                                                whiteSpace: 'nowrap',
                                                                borderLeft: `3px solid ${isExpired ? 'var(--danger)' : (task.assignedTo?.color || 'var(--accent)')}`,
                                                                backgroundColor: task.status === 'COMPLETED' ? 'var(--border-subtle)' : (isExpired ? '#fef2f2' : 'var(--bg-elevated)'),
                                                                color: task.status === 'COMPLETED' ? 'var(--text-tertiary)' : (isExpired ? 'var(--danger)' : 'var(--text-primary)'),
                                                                textDecoration: task.status === 'COMPLETED' ? 'line-through' : 'none',
                                                                cursor: 'pointer'
                                                            }}
                                                            title={isExpired ? "¡Rescate disponible! Esta tarea ha expirado." : ""}
                                                        >
                                                            {task.status === 'COMPLETED' && <span style={{ color: 'var(--success)', marginRight: '4px' }}><IconCheck /></span>}
                                                            {isExpired && '🚨 '}{task.title}
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )
                )
            )}
        </Card>
    );
}

/**
 * TaskListItem — Componente individual para tarea en lista
 */
function TaskListItem({ task, isExpired, onSelect, onEdit, onDelete, onToggleStatus }) {
    return (
        <div
            onClick={onSelect}
            style={{
                padding: 'var(--space-4)',
                borderRadius: 'var(--radius-lg)',
                backgroundColor: isExpired ? '#fef2f2' : 'var(--bg-elevated)',
                border: '1px solid var(--border-subtle)',
                borderLeft: `4px solid ${isExpired ? 'var(--danger)' : (task.assignedTo?.color || 'var(--border-default)')}`,
                cursor: 'pointer',
                transition: 'all 200ms var(--timing-smooth)',
                opacity: task.status === 'COMPLETED' ? 0.6 : 1,
            }}
        >
            <div style={{ display: 'flex', justifyDouble: 'space-between', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                    <h4
                        style={{
                            fontSize: 'var(--text-base)',
                            fontWeight: 'var(--font-bold)',
                            color: isExpired ? 'var(--danger)' : 'var(--text-primary)',
                            textDecoration: task.status === 'COMPLETED' ? 'line-through' : 'none',
                        }}
                    >
                        {task.status === 'COMPLETED' && <span style={{ color: 'var(--success)', marginRight: 'var(--space-2)' }}><IconCheck /></span>}
                        {task.title}
                        {isExpired && <span style={{ fontSize: '10px', fontWeight: 'black', marginLeft: 'var(--space-2)', color: 'var(--danger)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>🚨 Rescate disponible (-{task.points} pts)</span>}
                    </h4>
                    <p style={{ fontSize: 'var(--text-xs)', color: isExpired ? 'var(--danger)' : 'var(--text-secondary)', marginTop: 'var(--space-1)', opacity: isExpired ? 0.8 : 1 }}>
                        Responsable: {task.assignedTo?.username || 'Cualquiera'} | {task.points} 🪙 KPI
                    </p>
                </div>
                <div style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'center' }}>
                    <Button
                        variant={isExpired ? 'danger' : 'secondary'}
                        size="sm"
                        onClick={(e) => {
                            e.stopPropagation();
                            onToggleStatus();
                        }}
                    >
                        {task.status === 'PENDING' ? (isExpired ? '⚡ Rescatar' : <><IconCheck /> Hecho</>) : <><IconXMark /> Reabrir</>}
                    </Button>
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={(e) => {
                            e.stopPropagation();
                            onEdit();
                        }}
                    >
                        <IconPencil />
                    </Button>
                    <Button
                        variant="danger"
                        size="sm"
                        onClick={(e) => {
                            e.stopPropagation();
                            onDelete();
                        }}
                    >
                        <IconTrash />
                    </Button>
                </div>
            </div>
        </div>
    );
}

/**
 * TaskForm — Formulario para crear/editar tarea
 */
function TaskForm({ form, onFormChange, onSubmit, onCancel, isEditing, house }) {
    const toggleDay = (dayNum) => {
        const updated = form.specificDays.includes(dayNum)
            ? form.specificDays.filter(d => d !== dayNum)
            : [...form.specificDays, dayNum];
        onFormChange({ ...form, specificDays: updated });
    };

    const toggleParticipant = (id) => {
        const updated = form.participantIds.includes(id)
            ? form.participantIds.filter(p => p !== id)
            : [...form.participantIds, id];
        onFormChange({ ...form, participantIds: updated });
    };

    return (
        <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', marginBottom: 'var(--space-6)', padding: 'var(--space-4)', backgroundColor: 'var(--bg-elevated)', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-subtle)' }}>
            <Input
                id="task-title"
                label="Título"
                type="text"
                value={form.title}
                onChange={(e) => onFormChange({ ...form, title: e.target.value })}
                required
            />

            <Input
                id="task-description"
                label="Descripción / Instrucciones"
                type="text"
                value={form.description}
                onChange={(e) => onFormChange({ ...form, description: e.target.value })}
            />

            <Input
                id="task-points"
                label="Puntos KPI"
                type="number"
                value={form.points}
                onChange={(e) => onFormChange({ ...form, points: e.target.value })}
            />

            {!isEditing ? (
                <>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                        <label style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>Frecuencia / Tipo</label>
                        <select
                            value={form.rotationType}
                            onChange={(e) => onFormChange({ ...form, rotationType: e.target.value, specificDays: [], participantIds: [] })}
                            style={{ padding: 'var(--space-2)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-default)', fontSize: 'var(--text-sm)', backgroundColor: 'var(--bg-base)' }}
                        >
                            <option value="DAILY">Diaria</option>
                            <option value="WEEKLY">Semanal</option>
                            <option value="MONTHLY">Mensual</option>
                            <option value="FIXED">Fija</option>
                        </select>
                    </div>

                    {!isEditing && (
                        <Input
                            id="task-occurrences"
                            label="Turnos a proyectar hacia el futuro"
                            type="number"
                            value={form.occurrencesToProject}
                            onChange={(e) => onFormChange({ ...form, occurrencesToProject: e.target.value })}
                        />
                    )}

                    {form.rotationType === 'WEEKLY' && (
                        <div style={{ padding: 'var(--space-3)', backgroundColor: 'var(--bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
                            <span style={{ display: 'block', fontSize: '10px', fontWeight: 'bold', color: 'var(--text-tertiary)', marginBottom: 'var(--space-2)', textTransform: 'uppercase' }}>Selecciona los días específicos:</span>
                            <div style={{ display: 'flex', gap: '4px' }}>
                                {['L', 'M', 'X', 'J', 'V', 'S', 'D'].map((d, i) => (
                                    <button
                                        key={i}
                                        type="button"
                                        onClick={() => toggleDay(i + 1)}
                                        style={{
                                            width: 32, height: 32, borderRadius: 'var(--radius-md)', fontWeight: 'bold', fontSize: '11px', border: '1px solid var(--border-default)', cursor: 'pointer',
                                            backgroundColor: form.specificDays.includes(i + 1) ? 'var(--accent)' : 'var(--bg-elevated)',
                                            color: form.specificDays.includes(i + 1) ? '#fff' : 'var(--text-primary)'
                                        }}
                                    >
                                        {d}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                        <span style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>Distribución de Responsabilidades</span>
                        {form.rotationType === 'FIXED' ? (
                            <select
                                required
                                value={form.assignedToId}
                                onChange={(e) => onFormChange({ ...form, assignedToId: e.target.value })}
                                style={{ padding: 'var(--space-2)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-default)', fontSize: 'var(--text-sm)', backgroundColor: 'var(--bg-base)' }}
                            >
                                <option value="">-- Seleccionar encargado único --</option>
                                {house.members.map(m => <option key={m.userId} value={m.userId}>{m.username}</option>)}
                            </select>
                        ) : (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-2)' }}>
                                {house.members.map(m => (
                                    <label key={m.userId} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', padding: 'var(--space-2)', backgroundColor: 'var(--bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', fontSize: 'var(--text-xs)', cursor: 'pointer' }}>
                                        <input type="checkbox" checked={form.participantIds.includes(m.userId)} onChange={() => toggleParticipant(m.userId)} />
                                        <span>{m.username}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>
                </>
            ) : (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)', padding: 'var(--space-3)', backgroundColor: 'var(--bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                        <label style={{ fontSize: '10px', fontWeight: 'bold', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Asignado actual</label>
                        <select
                            value={form.assignedToId}
                            onChange={(e) => onFormChange({ ...form, assignedToId: e.target.value })}
                            style={{ padding: 'var(--space-2)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-default)', fontSize: 'var(--text-sm)', backgroundColor: 'var(--bg-base)' }}
                        >
                            <option value="">-- Sin asignar --</option>
                            {house.members.map(m => <option key={m.userId} value={m.userId}>{m.username}</option>)}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                        <label style={{ fontSize: '10px', fontWeight: 'bold', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Modificar Fecha límite</label>
                        <input type="date" value={form.startDate} onChange={(e) => onFormChange({ ...form, startDate: e.target.value })} style={{ padding: 'var(--space-2)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-default)', fontSize: 'var(--text-sm)', backgroundColor: 'var(--bg-base)' }} />
                    </div>
                </div>
            )}

            <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                <Button variant="secondary" size="md" type="button" onClick={onCancel} full>
                    Cancelar
                </Button>
                <Button variant="primary" size="md" type="submit" full>
                    {isEditing ? 'Guardar Cambios' : 'Proyectar Agenda'}
                </Button>
            </div>
        </form>
    );
}

/**
 * ExpensesSection — Sección de gestión de gastos
 */
function ExpensesSection({
                             expenses,
                             settlements,
                             memberStatuses,
                             house,
                             showExpenseForm,
                             expenseForm,
                             onSubmitExpense,
                             onCancelExpense,
                             onAddExpense,
                             onSettlePayment,
                             setExpenseForm,
                             currentUserId,
                         }) {
    const toggleBeneficiary = (id) => {
        const updated = expenseForm.participantIds.includes(id)
            ? expenseForm.participantIds.filter(p => p !== id)
            : [...expenseForm.participantIds, id];
        setExpenseForm({ ...expenseForm, participantIds: updated });
    };

    return (
        <Card padding="lg">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
                <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <IconBanknotes /> Gastos & Liquidaciones
                    </span>
                </h2>
                {!showExpenseForm && (
                    <Button variant="primary" size="sm" onClick={onAddExpense}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><IconPlus /> Subir Gasto</span>
                    </Button>
                )}
            </div>

            {/* Capa Activa de Liquidaciones cruzadas recomendadas */}
            {settlements.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', marginBottom: 'var(--space-5)' }}>
                    {settlements.map((s, idx) => (
                        <div key={idx} style={{ backgroundColor: 'var(--accent-ultra-light)', border: '1px solid var(--accent-light)', padding: 'var(--space-3)', borderRadius: 'var(--radius-xl)', display: 'flex', justifyDouble: 'space-between', alignItems: 'center', justifyContent: 'space-between', fontSize: 'var(--text-xs)' }}>
                            <p style={{ margin: 0, color: 'var(--text-primary)' }}><span style={{ color: 'var(--success)' }}><IconBanknotes /></span> <strong>{s.debtorUsername}</strong> debe pagar <strong>{s.amount.toFixed(2)}€</strong> a <strong>{s.creditorUsername}</strong>.</p>
                            <Button variant="primary" size="xs" onClick={() => onSettlePayment(s.debtorId, s.debtorUsername, s.creditorId, s.creditorUsername, s.amount)}>Registrar Pago</Button>
                        </div>
                    ))}
                </div>
            )}

            {showExpenseForm && (
                <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', backgroundColor: 'var(--bg-elevated)', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-subtle)' }}>
                    <form onSubmit={onSubmitExpense} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
                        <h3 style={{ fontSize: '11px', fontWeight: 'bold', color: 'var(--accent)', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '8px' }}><IconReceiptRefund /> Desglosar Factura</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)' }}>
                            <Input
                                id="exp-title"
                                label="Concepto / Descripción"
                                type="text"
                                required
                                value={expenseForm.title}
                                onChange={(e) => setExpenseForm({ ...expenseForm, title: e.target.value })}
                            />
                            <Input
                                id="exp-amount"
                                label="Importe total (€)"
                                type="number"
                                step="0.01"
                                required
                                value={expenseForm.amount}
                                onChange={(e) => setExpenseForm({ ...expenseForm, amount: e.target.value })}
                            />
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                            <span style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>¿Entre quiénes se divide el gasto? (Beneficiarios)</span>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-2)' }}>
                                {house.members.map(m => (
                                    <label key={m.userId} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', padding: 'var(--space-2)', backgroundColor: 'var(--bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', fontSize: 'var(--text-xs)', cursor: 'pointer' }}>
                                        <input type="checkbox" checked={expenseForm.participantIds.includes(m.userId)} onChange={() => toggleBeneficiary(m.userId)} />
                                        <span>{m.username}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                            <Button variant="secondary" size="md" type="button" onClick={onCancelExpense} full>
                                Cancelar
                            </Button>
                            <Button variant="primary" size="md" type="submit" full>
                                Subir y Repartir
                            </Button>
                        </div>
                    </form>
                </div>
            )}

            {expenses.length === 0 && !showExpenseForm ? (
                <div style={{ textAlign: 'center', padding: 'var(--space-8)' }}>
                    <p style={{ color: 'var(--text-tertiary)' }}>
                        No hay gastos registrados.
                    </p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                    {expenses.map((expense) => (
                        <div
                            key={expense.id}
                            style={{
                                padding: 'var(--space-4)',
                                borderRadius: 'var(--radius-lg)',
                                backgroundColor: 'var(--bg-elevated)',
                                border: '1px solid var(--border-subtle)',
                                borderLeft: '4px solid var(--success)',
                            }}
                        >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <h4 style={{ fontWeight: 'var(--font-bold)', fontSize: 'var(--text-sm)' }}>
                                        {expense.title}
                                    </h4>
                                    <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', marginTop: '2px' }}>
                                        Puso: <strong>{expense.paidByUsername}</strong> | Para: {expense.participantUsernames?.join(', ')}
                                    </p>
                                </div>
                                <span style={{ fontSize: 'var(--text-base)', fontWeight: 'var(--font-black)', color: 'var(--text-primary)' }}>
                                    {expense.amount.toFixed(2)}€
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </Card>
    );
}

/**
 * HistorySection — Sección de transparencia y auditoría
 */
function HistorySection({ activityLogs }) {
    return (
        <Card padding="lg">
            <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)', marginBottom: 'var(--space-4)' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <IconMagnifyingGlass /> Transparencia & Auditoría
                </span>
            </h2>

            {activityLogs.length === 0 ? (
                <p style={{ color: 'var(--text-tertiary)', textAlign: 'center', padding: 'var(--space-8)' }}>
                    Sin registro de actividades todavía.
                </p>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                    {activityLogs.map((log) => (
                        <div
                            key={log.id || log.createdAt}
                            style={{
                                padding: 'var(--space-3)',
                                borderRadius: 'var(--radius-lg)',
                                backgroundColor: 'var(--bg-elevated)',
                                border: '1px solid var(--border-subtle)',
                                borderLeft: `4px solid ${log.actionType === 'CREATE' ? 'var(--success)' : log.actionType === 'DELETE' ? 'var(--danger)' : 'var(--accent)'}`,
                            }}
                        >
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                <span style={{ fontSize: '10px', color: 'var(--text-tertiary)' }}>
                                    {new Date(log.createdAt).toLocaleString('es-ES')}
                                </span>
                                <Badge variant="secondary" size="xs">{log.actionType}</Badge>
                            </div>
                            <p style={{ fontSize: 'var(--text-sm)', color: 'var(--text-primary)', margin: 0, fontWeight: 'medium' }}>
                                {log.description || log.message}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </Card>
    );
}

/**
 * EditHouseModal — Modal para editar configuración de vivienda
 */
function EditHouseModal({ house, form, onFormChange, onSubmit, onClose }) {
    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(0,0,0,0.5)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1000,
                padding: 'var(--space-4)',
            }}
        >
            <Card padding="lg" style={{ maxWidth: '400px', width: '100%' }}>
                <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)', marginBottom: 'var(--space-4)' }}>
                    Editar Vivienda
                </h2>
                <form
                    onSubmit={onSubmit}
                    style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}
                >
                    <Input
                        id="edit-house-name"
                        label="Nombre"
                        type="text"
                        value={form.name}
                        onChange={(e) => onFormChange({ ...form, name: e.target.value })}
                    />
                    <Input
                        id="edit-house-pic"
                        label="URL de la Foto de Perfil"
                        type="text"
                        value={form.profilePictureUrl}
                        onChange={(e) => onFormChange({ ...form, profilePictureUrl: e.target.value })}
                    />
                    <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                        <Button variant="secondary" size="md" type="button" onClick={onClose} full>
                            Cancelar
                        </Button>
                        <Button variant="primary" size="md" type="submit" full>
                            Guardar
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
}

/**
 * EditProfileModal — Modal especializado para la mutación de datos de usuario
 */
function EditProfileModal({ form, onFormChange, onSubmit, onClose }) {
    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(0,0,0,0.5)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1200,
                padding: 'var(--space-4)',
            }}
        >
            <Card padding="lg" style={{ maxWidth: '400px', width: '100%' }}>
                <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)', marginBottom: 'var(--space-4)' }}>
                    Editar Mi Perfil
                </h2>
                <form
                    onSubmit={onSubmit}
                    style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}
                >
                    <Input
                        id="edit-profile-username"
                        label="Nuevo Nombre de Usuario"
                        type="text"
                        required
                        value={form.username}
                        onChange={(e) => onFormChange({ ...form, username: e.target.value })}
                    />
                    <Input
                        id="edit-profile-pic"
                        label="URL de mi Foto de Perfil"
                        type="text"
                        value={form.profilePictureUrl}
                        onChange={(e) => onFormChange({ ...form, profilePictureUrl: e.target.value })}
                    />
                    <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                        <Button variant="secondary" size="md" type="button" onClick={onClose} full>
                            Cancelar
                        </Button>
                        <Button variant="primary" size="md" type="submit" full>
                            Guardar Perfil
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
}

/**
 * CustomDialog — Diálogo personalizado para alertas y confirmaciones
 */
function CustomDialog({ title, message, type, onClose, onConfirm }) {
    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(0,0,0,0.65)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1100,
            }}
        >
            <Card padding="lg" style={{ maxWidth: '400px' }}>
                <h2 style={{ fontSize: 'var(--text-lg)', fontWeight: 'var(--font-bold)', marginBottom: 'var(--space-2)' }}>
                    {title}
                </h2>
                <p style={{ color: 'var(--text-secondary)', marginBottom: 'var(--space-4)', fontSize: 'var(--text-sm)' }}>
                    {message}
                </p>
                <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                    {type === 'confirm' && (
                        <>
                            <Button variant="secondary" size="md" onClick={onClose} full>
                                Cancelar
                            </Button>
                            <Button variant="primary" size="md" onClick={onConfirm} full>
                                Confirmar
                            </Button>
                        </>
                    )}
                    {type === 'alert' && (
                        <Button variant="primary" size="md" onClick={onClose} full>
                            Aceptar
                        </Button>
                    )}
                </div>
            </Card>
        </div>
    );
}

/**
 * TaskDetailModal — Modal con detalles de una tarea
 */
function TaskDetailModal({ task, onClose, onEdit, onDelete }) {
    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(0,0,0,0.5)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1000,
            }}
        >
            <Card
                padding="lg"
                style={{
                    maxWidth: '500px',
                    width: '100%',
                    borderLeft: `4px solid ${task.assignedTo?.color || 'var(--accent)'}`,
                }}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 'var(--space-4)' }}>
                    <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 'var(--font-bold)' }}>
                        {task.title}
                    </h2>
                    <Button variant="ghost" size="sm" onClick={onClose}>
                        <IconXMark />
                    </Button>
                </div>

                <div style={{ marginBottom: 'var(--space-4)' }}>
                    {task.description && (
                        <p style={{ color: 'var(--text-secondary)', marginBottom: 'var(--space-3)', fontSize: 'var(--text-sm)' }}>
                            {task.description}
                        </p>
                    )}
                    <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
                        <Badge variant="primary">{task.points} KPI</Badge>
                        <Badge variant={task.status === 'COMPLETED' ? 'success' : 'warning'}>
                            {task.status === 'COMPLETED' ? 'Completado' : 'Pendiente'}
                        </Badge>
                    </div>
                </div>

                <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: 'var(--space-4)', display: 'flex', gap: 'var(--space-2)' }}>
                    <Button variant="secondary" size="sm" onClick={onEdit} full>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}><IconPencil /> Editar</span>
                    </Button>
                    <Button variant="danger" size="sm" onClick={onDelete} full>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}><IconTrash /> Eliminar</span>
                    </Button>
                    <Button variant="primary" size="sm" onClick={onClose} full>
                        Cerrar
                    </Button>
                </div>
            </Card>
        </div>
    );
}