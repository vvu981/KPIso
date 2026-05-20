import { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../../context/authContextValue.js';
import api from '../../api/client';
import { Button } from '../ui/Button.jsx';
import { Card } from '../ui/Card.jsx';
import { Badge } from '../ui/Badge.jsx';
import { Alert } from '../ui/Alert.jsx';
import { Input } from '../ui/Input.jsx';

const IconPlus = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <line x1="12" y1="5" x2="12" y2="19"/>
        <line x1="5" y1="12" x2="19" y2="12"/>
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

const IconCheck = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12"/>
    </svg>
);

const IconChevronDown = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="6 9 12 15 18 9"/>
    </svg>
);

const IconChevronUp = () => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="18 15 12 9 6 15"/>
    </svg>
);

/**
 * ShoppingListSection — Componente de Lista de la Compra Inteligente
 * * Características:
 * - Añadir productos con búsqueda en Open Food Facts (con soporte manual fallback)
 * - Panel de presupuesto estimado
 * - Listado de productos PENDING (sin comprar)
 * - Historial colapsable de productos BOUGHT (comprados e inmutables)
 */
export default function ShoppingListSection({ houseId, currentUserId, onPurchaseRegistered }) {
    const authContext = useContext(AuthContext);
    const { token } = authContext || {};
    const searchInputRef = useRef(null);
    const inputWrapperRef = useRef(null);
    const searchTimeoutRef = useRef(null);
    const [dropdownCoords, setDropdownCoords] = useState({ top: 0, left: 0, width: 0 });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [productInput, setProductInput] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [searchLoading, setSearchLoading] = useState(false);
    const [shoppingList, setShoppingList] = useState({
        pendingItems: [],
        boughtItems: [],
        estimatedBudget: 0
    });
    const [historyExpanded, setHistoryExpanded] = useState(false);
    const [loadingItemId, setLoadingItemId] = useState(null);
    const [houseMembers, setHouseMembers] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [showCheckout, setShowCheckout] = useState(false);
    const [checkoutAmount, setCheckoutAmount] = useState('');
    const [checkoutPayer, setCheckoutPayer] = useState(currentUserId);
    const [checkoutLoading, setCheckoutLoading] = useState(false);
    const [openDropdownId, setOpenDropdownId] = useState(null);
    const [manualMode, setManualMode] = useState(false);
    const [manualName, setManualName] = useState('');
    const [manualPrice, setManualPrice] = useState('');

    const getAssigneesLabel = (assignedUserIds) => {
        if (!assignedUserIds || assignedUserIds.length === 0 || assignedUserIds.length === houseMembers.length) {
            return 'Todos';
        }
        const assignedNames = houseMembers
            .filter(m => assignedUserIds.includes(m.userId))
            .map(m => m.username);
        if (assignedNames.length === 0) return 'Ninguno';
        if (assignedNames.length <= 2) return assignedNames.join(', ');
        return `${assignedNames[0]} y ${assignedNames.length - 1} más`;
    };

    // Cargar lista de compra al montar o cuando cambie houseId
    useEffect(() => {
        loadShoppingList();
    }, [houseId]);

    // Actualizar coordenadas del dropdown cuando se muestra o está cargando
    useEffect(() => {
        if ((showSuggestions || searchLoading) && inputWrapperRef.current) {
            const rect = inputWrapperRef.current.getBoundingClientRect();
            setDropdownCoords({
                top: rect.bottom + 4,
                left: rect.left,
                width: rect.width,
            });
        }
    }, [showSuggestions, searchLoading, suggestions]);

    // Limpieza del temporizador de búsqueda al desmontar el componente
    useEffect(() => {
        return () => {
            if (searchTimeoutRef.current) {
                clearTimeout(searchTimeoutRef.current);
            }
        };
    }, []);

    /**
     * Busca sugerencias de productos mientras el usuario escribe
     */
    const handleSearchProduct = (query) => {
        setProductInput(query);

        // Cancelar el temporizador anterior
        if (searchTimeoutRef.current) {
            clearTimeout(searchTimeoutRef.current);
        }

        if (!query.trim() || query.trim().length < 2) {
            setSuggestions([]);
            setShowSuggestions(false);
            setSearchLoading(false);
            return;
        }

        // Mostrar el estado de carga inmediatamente para dar feedback
        setSearchLoading(true);
        setError(null);

        // Programar la búsqueda con 300ms de retraso (Debounce)
        searchTimeoutRef.current = setTimeout(async () => {
            try {
                const response = await api.get(`/shopping-list/search?query=${encodeURIComponent(query.trim())}`, {
                    headers: token ? { Authorization: `Bearer ${token}` } : {}
                });

                setSuggestions(response.data.slice(0, 8));
                setShowSuggestions(response.data.length > 0);
            } catch (err) {
                console.error('Error al buscar sugerencias:', err);
                setSuggestions([]);
                setShowSuggestions(false);
            } finally {
                setSearchLoading(false);
            }
        }, 300);
    };

    /**
     * Limpia el buscador y restablece el estado de sugerencias
     */
    const handleClearSearch = () => {
        setProductInput('');
        setSuggestions([]);
        setShowSuggestions(false);
        setSearchLoading(false);
        if (searchTimeoutRef.current) {
            clearTimeout(searchTimeoutRef.current);
        }
        // Restaurar foco al input nativo
        if (searchInputRef.current) {
            searchInputRef.current.focus();
        }
    };

    /**
     * Carga la lista de compra completa desde el backend
     */
    const loadShoppingList = async () => {
        setLoading(true);
        setError(null);
        try {
            const [listRes, houseRes] = await Promise.all([
                api.get(`/shopping-list/${houseId}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} }),
                api.get(`/houses/${houseId}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
            ]);
            setShoppingList(listRes.data);
            setHouseMembers(houseRes.data.members || []);
        } catch (err) {
            console.error('Error al cargar la lista de compra:', err);
            setError('No se pudo cargar la lista de compra. Intenta de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    /**
     * Añade un nuevo producto a la lista (ya sea desde input directo o desde sugerencia)
     */
    const handleAddProduct = async (e, suggestion = null) => {
        e?.preventDefault();
        const productName = suggestion ? suggestion.name : (manualMode ? manualName.trim() : productInput.trim());
        if (!productName) {
            setError('Por favor escribe un nombre de producto');
            return;
        }
        setLoading(true);
        setError(null);
        setSuccess(null);
        setShowSuggestions(false);
        try {
            const payload = {
                productName: productName,
                houseId: houseId,
                addedById: currentUserId,
                assignedUserIds: []
            };
            if (manualMode && manualPrice && !isNaN(parseFloat(manualPrice))) {
                payload.manualPrice = parseFloat(manualPrice);
            }

            const response = await api.post('/shopping-list/add', payload, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            setShoppingList(prev => ({
                ...prev,
                pendingItems: [response.data, ...prev.pendingItems],
                estimatedBudget: prev.estimatedBudget + (response.data.estimatedPrice || 0)
            }));
            setProductInput('');
            setManualName('');
            setManualPrice('');
            setManualMode(false);
            setSuccess(`Producto añadido a la lista`);
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            console.error('Error al añadir producto:', err);
            setError(err.response?.data || 'Error al añadir el producto. Intenta de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    const handleCheckout = async (e) => {
        if (e) e.preventDefault();
        setCheckoutLoading(true);
        setError(null);
        try {
            await api.post(`/shopping-list/checkout`, {
                houseId,
                paidById: checkoutPayer,
                totalRealAmount: parseFloat(checkoutAmount)
            }, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            setSuccess("Compra finalizada y gastos registrados correctamente.");
            setShowCheckout(false);
            setCheckoutAmount('');
            loadShoppingList();
            if (onPurchaseRegistered) {
                onPurchaseRegistered();
            }
        } catch (err) {
            setError(err.response?.data || 'Error al finalizar la compra.');
        } finally {
            setCheckoutLoading(false);
        }
    };

    /**
     * Marca un ítem como comprado
     */
    const handleMarkAsBought = async (itemId, itemName) => {
        setLoadingItemId(itemId);
        setError(null);

        try {
            const response = await api.put(`/shopping-list/${itemId}/mark-bought`, {}, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });

            // Actualizar la lista: mover de pending a bought
            setShoppingList(prev => ({
                ...prev,
                pendingItems: prev.pendingItems.filter(item => item.id !== itemId),
                boughtItems: [response.data, ...prev.boughtItems],
                estimatedBudget: prev.estimatedBudget - response.data.estimatedPrice
            }));

            setSuccess(`"${itemName}" marcado como comprado`);
            setTimeout(() => setSuccess(null), 2500);
        } catch (err) {
            console.error('Error al marcar como comprado:', err);
            setError('No se pudo marcar el producto como comprado.');
        } finally {
            setLoadingItemId(null);
        }
    };

    /**
     * Elimina un ítem de la lista. Solo permitido si no ha sido comprado.
     */
    const handleDeleteItem = async (itemId, itemName, isPending) => {
        if (!isPending) {
            setError("Los productos ya comprados son parte del historial financiero y no se pueden eliminar.");
            return;
        }

        if (!confirm(`¿Estás seguro de que quieres eliminar "${itemName}"?`)) {
            return;
        }

        setLoadingItemId(itemId);
        setError(null);

        try {
            await api.delete(`/shopping-list/${itemId}`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });

            // Actualizar la lista
            const deletedItem = shoppingList.pendingItems.find(item => item.id === itemId);
            setShoppingList(prev => ({
                ...prev,
                pendingItems: prev.pendingItems.filter(item => item.id !== itemId),
                estimatedBudget: prev.estimatedBudget - (deletedItem?.estimatedPrice || 0)
            }));

            setSuccess(`"${itemName}" eliminado`);
            setTimeout(() => setSuccess(null), 2500);
        } catch (err) {
            console.error('Error al eliminar producto:', err);
            setError('No se pudo eliminar el producto.');
        } finally {
            setLoadingItemId(null);
        }
    };

    /**
     * Actualiza los usuarios asignados a un producto ya existente
     */
    const handleUpdateItemAssignees = async (itemId, assigneeIds) => {
        try {
            const response = await api.put(`/shopping-list/${itemId}/assignees`, assigneeIds, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });

            // Actualizar el estado local
            setShoppingList(prev => ({
                ...prev,
                pendingItems: prev.pendingItems.map(item => 
                    item.id === itemId ? { ...item, assignedUserIds: response.data.assignedUserIds } : item
                )
            }));
        } catch (err) {
            console.error('Error al actualizar asignaciones del producto:', err);
            setError('No se pudieron actualizar los usuarios asignados.');
        }
    };

    const handleToggleMember = (item, memberId) => {
        const currentAssignees = item.assignedUserIds || [];
        let newAssignees;
        
        if (currentAssignees.length === 0) {
            newAssignees = houseMembers
                .map(m => m.userId)
                .filter(id => id !== memberId);
        } else {
            if (currentAssignees.includes(memberId)) {
                newAssignees = currentAssignees.filter(id => id !== memberId);
            } else {
                newAssignees = [...currentAssignees, memberId];
            }
        }
        handleUpdateItemAssignees(item.id, newAssignees);
    };

    if (loading && shoppingList.pendingItems.length === 0) {
        return (
            <div className="flex items-center justify-center py-12">
                <div className="text-center text-gray-500">
                    <div className="inline-block animate-spin mb-2">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" opacity="0.3"/>
                            <path d="M22 12a10 10 0 0 0-10-10" strokeLinecap="round" opacity="0.7"/>
                        </svg>
                    </div>
                    <p>Cargando lista de compra...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Mensajes de Error y Éxito */}
            {error && (
                <Alert type="error" title="Error">
                    {error}
                </Alert>
            )}
            {success && (
                <Alert type="success" title="Éxito">
                    {success}
                </Alert>
            )}

            {/* Formulario de Añadir Producto */}
            <Card>
                <div className="p-5 border-b border-gray-200">
                    <h3 className="text-lg font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>Añadir Producto</h3>
                    <form onSubmit={handleAddProduct} className="relative">
                        <div className="flex gap-3">
                            <div className="flex-grow relative" ref={inputWrapperRef}>
                                <Input
                                    ref={searchInputRef}
                                    type="text"
                                    placeholder="Ej: Leche, Pan integral, Tomates..."
                                    value={productInput}
                                    onChange={(e) => handleSearchProduct(e.target.value)}
                                    onFocus={() => productInput.trim().length >= 2 && setSuggestions(suggestions.length > 0 ? suggestions : []) && setShowSuggestions(suggestions.length > 0)}
                                    disabled={loading}
                                    autoComplete="off"
                                    style={{ paddingRight: '36px' }}
                                />

                                {productInput && (
                                    <button
                                        type="button"
                                        onClick={handleClearSearch}
                                        style={{
                                            position: 'absolute',
                                            right: '12px',
                                            top: '50%',
                                            transform: 'translateY(-50%)',
                                            background: 'transparent',
                                            border: 'none',
                                            cursor: 'pointer',
                                            color: 'var(--text-secondary)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            padding: '4px',
                                            borderRadius: '50%',
                                            transition: 'color 0.15s, background 0.15s',
                                        }}
                                        onMouseEnter={e => {
                                            e.currentTarget.style.color = 'var(--text-primary)';
                                            e.currentTarget.style.background = 'var(--bg-elevated)';
                                        }}
                                        onMouseLeave={e => {
                                            e.currentTarget.style.color = 'var(--text-secondary)';
                                            e.currentTarget.style.background = 'transparent';
                                        }}
                                        title="Limpiar búsqueda"
                                    >
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                        </svg>
                                    </button>
                                )}

                                {/* Dropdown de Sugerencias — fixed para escapar del overflow:hidden de la Card */}
                                {showSuggestions && suggestions.length > 0 && (
                                    <div
                                        style={{
                                            position: 'fixed',
                                            top: dropdownCoords.top,
                                            left: dropdownCoords.left,
                                            width: dropdownCoords.width,
                                            zIndex: 9999,
                                            background: 'var(--bg-surface)',
                                            border: '1px solid var(--border-default)',
                                            borderRadius: 'var(--radius-lg)',
                                            boxShadow: 'var(--shadow-lg)',
                                            maxHeight: '22rem',
                                            overflowY: 'auto',
                                        }}
                                    >
                                        {suggestions.map((suggestion, idx) => (
                                            <button
                                                key={idx}
                                                type="button"
                                                onClick={(e) => {
                                                    e.preventDefault();
                                                    handleAddProduct(e, suggestion);
                                                }}
                                                style={{
                                                    width: '100%',
                                                    textAlign: 'left',
                                                    padding: '10px 14px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '12px',
                                                    background: 'transparent',
                                                    border: 'none',
                                                    borderBottom: '1px solid var(--border-subtle)',
                                                    cursor: 'pointer',
                                                    transition: 'background 0.15s',
                                                    color: 'var(--text-primary)',
                                                }}
                                                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-elevated)'}
                                                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                                            >
                                                <img
                                                    src={suggestion.imageUrl}
                                                    alt={suggestion.name}
                                                    style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', flexShrink: 0 }}
                                                    onError={(e) => { e.target.src = 'https://via.placeholder.com/40?text=?'; }}
                                                />
                                                <div style={{ flexGrow: 1, minWidth: 0 }}>
                                                    <p style={{ fontWeight: 600, fontSize: 'var(--text-sm)', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', margin: 0 }}>
                                                        {suggestion.name}
                                                    </p>
                                                    <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', margin: 0 }}>
                                                        Est. {suggestion.estimatedPrice.toFixed(2)}€
                                                    </p>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                )}

                                {searchLoading && productInput.trim().length >= 2 && (
                                    <div
                                        style={{
                                            position: 'fixed',
                                            top: dropdownCoords.top,
                                            left: dropdownCoords.left,
                                            width: dropdownCoords.width,
                                            zIndex: 9999,
                                            background: 'var(--bg-surface)',
                                            border: '1px solid var(--border-default)',
                                            borderRadius: 'var(--radius-lg)',
                                            boxShadow: 'var(--shadow-lg)',
                                            padding: '12px 16px',
                                            textAlign: 'center',
                                            fontSize: 'var(--text-sm)',
                                            color: 'var(--text-secondary)',
                                        }}
                                    >
                                        Buscando...
                                    </div>
                                )}
                            </div>
                            <div className="flex items-center gap-2">
                                <button
                                    type="button"
                                    onClick={() => setManualMode(!manualMode)}
                                    className={`px-3 py-1.5 rounded-md border text-sm font-medium ${manualMode ? 'bg-gray-100' : 'bg-white'}`}
                                >
                                    {manualMode ? 'Modo Manual' : 'Autocompletar'}
                                </button>
                                <Button
                                type="submit"
                                disabled={loading || !productInput.trim()}
                                className="flex items-center gap-2 whitespace-nowrap"
                            >
                                <IconPlus />
                                Añadir
                            </Button>
                            </div>
                        </div>
                    </form>

                    {/* Formulario Manual Extra */}
                    {manualMode && (
                        <form onSubmit={handleAddProduct} className="mt-3">
                            <div className="flex gap-3">
                                <Input
                                    type="text"
                                    placeholder="Nombre del producto"
                                    value={manualName}
                                    onChange={(e) => { setManualName(e.target.value); setProductInput(e.target.value); }}
                                    disabled={loading}
                                />
                                <Input
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    placeholder="Precio (€) opcional"
                                    value={manualPrice}
                                    onChange={(e) => setManualPrice(e.target.value)}
                                    disabled={loading}
                                />
                                <Button
                                    type="submit"
                                    disabled={loading || !manualName.trim()}
                                    className="flex items-center gap-2 whitespace-nowrap"
                                >
                                    <IconPlus />
                                    Añadir Manual
                                </Button>
                            </div>
                        </form>
                    )}
                </div>
            </Card>



            {/* Lista de Productos Pendientes */}
            {shoppingList.pendingItems.length > 0 ? (
                <Card>
                    <div className="p-5 border-b border-gray-200">
                        <h3 className="text-lg font-semibold" style={{ color: 'var(--text-primary)' }}>
                            Productos por Comprar ({shoppingList.pendingItems.length})
                        </h3>
                    </div>
                    <div className="divide-y divide-gray-200">
                        {shoppingList.pendingItems.map(item => (
                            <div
                                key={item.id}
                                className="p-4 hover:bg-gray-50 transition-colors flex items-center gap-4"
                            >
                                {/* Imagen del Producto */}
                                <div className="flex-shrink-0">
                                    {item.imageUrl && item.imageUrl.trim() ? (
                                        <img
                                            src={item.imageUrl}
                                            alt={item.name}
                                            className="w-16 h-16 object-cover rounded-md bg-gray-100 border border-gray-200"
                                            onError={(e) => { e.target.style.display = 'none'; }}
                                        />
                                    ) : (
                                        <div className="w-16 h-16 flex items-center justify-center rounded-md bg-indigo-500 text-white font-bold text-xl">
                                            {item.name && item.name.charAt(0).toUpperCase()}
                                        </div>
                                    )}
                                </div>

                                {/* Información del Producto */}
                                <div className="flex-grow min-w-0">
                                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                        <div>
                                            <p className="font-medium truncate text-base" style={{ color: 'var(--text-primary)' }}>
                                                {item.name}
                                            </p>
                                            <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
                                                Estimado: <span className="font-semibold text-blue-600">{item.estimatedPrice.toFixed(2)}€</span>
                                            </p>
                                        </div>

                                        {/* Selector de convivientes asignados al producto */}
                                        <div className="relative flex items-center gap-1.5 mt-2 md:mt-0">
                                            <span className="text-xs text-gray-500 font-medium">Para:</span>
                                            <div className="relative">
                                                <button
                                                    type="button"
                                                    onClick={() => setOpenDropdownId(openDropdownId === item.id ? null : item.id)}
                                                    className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium border bg-white border-gray-300 text-gray-700 hover:bg-gray-50 focus:outline-none transition-colors"
                                                >
                                                    <span className="truncate max-w-[120px]">{getAssigneesLabel(item.assignedUserIds)}</span>
                                                    <IconChevronDown />
                                                </button>

                                                {openDropdownId === item.id && (
                                                    <>
                                                        <div 
                                                            style={{ position: 'fixed', inset: 0, zIndex: 40 }} 
                                                            onClick={() => setOpenDropdownId(null)}
                                                        />
                                                        <div 
                                                            className="absolute right-0 mt-1 bg-white border border-gray-200 rounded-md shadow-lg p-2 min-w-[180px] z-50 flex flex-col gap-1"
                                                        >
                                                            <div className="px-2 py-1 text-[10px] uppercase tracking-wider font-semibold text-gray-400 border-b border-gray-100 mb-1">
                                                                Asignar a:
                                                            </div>
                                                            {houseMembers.map(member => {
                                                                const checked = !item.assignedUserIds || item.assignedUserIds.length === 0 || item.assignedUserIds.includes(member.userId);
                                                                return (
                                                                    <label 
                                                                        key={member.userId} 
                                                                        className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-gray-50 cursor-pointer text-xs select-none"
                                                                    >
                                                                        <input 
                                                                            type="checkbox"
                                                                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 h-3.5 w-3.5"
                                                                            checked={checked}
                                                                            onChange={() => handleToggleMember(item, member.userId)}
                                                                        />
                                                                        <span className="text-gray-700 font-medium">{member.username}</span>
                                                                    </label>
                                                                );
                                                            })}
                                                        </div>
                                                    </>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* Botones de Acción */}
                                <div className="flex-shrink-0 flex gap-2">
                                    <Button
                                        variant="danger"
                                        size="sm"
                                        onClick={() => handleDeleteItem(item.id, item.name, true)}
                                        disabled={loadingItemId === item.id}
                                        title="Eliminar"
                                        className="flex items-center gap-1"
                                    >
                                        <IconTrash />
                                    </Button>
                                </div>
                            </div>
                        ))}
                    </div>
                    {/* Card Footer with Budget and Checkout Action */}
                    <div className="p-5 bg-gray-50 border-t border-gray-200 flex flex-col sm:flex-row justify-between items-center gap-4">
                        <div className="flex items-baseline gap-2">
                            <span className="text-sm font-medium text-gray-500">Presupuesto Estimado:</span>
                            <span className="text-2xl font-bold text-blue-600">
                                {shoppingList.estimatedBudget.toFixed(2)}€
                            </span>
                            <span className="text-xs text-gray-400">
                                ({shoppingList.pendingItems.length} {shoppingList.pendingItems.length === 1 ? 'producto' : 'productos'})
                            </span>
                        </div>
                        <Button 
                            variant="primary" 
                            onClick={() => setShowCheckout(true)}
                            className="w-full sm:w-auto px-6 py-2.5 text-sm font-semibold shadow-sm hover:shadow transition-all"
                        >
                            Pagar compra
                        </Button>
                    </div>
                </Card>
            ) : (
                <Card>
                    <div className="p-8 text-center">
                        <p className="text-gray-500">
                            No hay productos en la lista. ¡Añade el primero!
                        </p>
                    </div>
                </Card>
            )}

            {/* Historial de Productos Comprados (Colapsable) */}
            {shoppingList.boughtItems.length > 0 && (() => {
                // Agrupar boughtItems por checkoutId o fallback a fecha si no tiene checkoutId (legacy)
                const groups = {};
                shoppingList.boughtItems.forEach(item => {
                    const key = item.checkoutId || `legacy-${item.updatedAt?.split('T')[0] || item.createdAt?.split('T')[0] || 'legacy'}`;
                    if (!groups[key]) {
                        groups[key] = {
                            id: key,
                            date: item.updatedAt || item.createdAt,
                            items: []
                        };
                    }
                    groups[key].items.push(item);
                });
                const sortedGroups = Object.values(groups).sort((a, b) => new Date(b.date) - new Date(a.date));

                const formatDate = (dateStr) => {
                    if (!dateStr) return 'Fecha desconocida';
                    const date = new Date(dateStr);
                    if (isNaN(date.getTime())) return 'Fecha desconocida';
                    const day = String(date.getDate()).padStart(2, '0');
                    const month = String(date.getMonth() + 1).padStart(2, '0');
                    const year = date.getFullYear();
                    const hours = String(date.getHours()).padStart(2, '0');
                    const minutes = String(date.getMinutes()).padStart(2, '0');
                    return `${day}/${month}/${year} a las ${hours}:${minutes}`;
                };

                return (
                    <Card>
                        <button
                            onClick={() => setHistoryExpanded(!historyExpanded)}
                            className="w-full p-5 border-b border-gray-200 flex items-center justify-between hover:bg-gray-50 transition-colors"
                        >
                            <h3 className="text-lg font-semibold" style={{ color: 'var(--text-primary)' }}>
                                Historial de Compras ({shoppingList.boughtItems.length})
                            </h3>
                            {historyExpanded ? <IconChevronUp /> : <IconChevronDown />}
                        </button>

                        {historyExpanded && (
                            <div className="p-5 flex flex-col gap-6" style={{ backgroundColor: 'var(--bg-base)' }}>
                                {sortedGroups.map(group => (
                                    <div 
                                        key={group.id} 
                                        style={{
                                            backgroundColor: 'var(--bg-surface)',
                                            border: '1px solid var(--border-subtle)',
                                            borderRadius: 'var(--radius-lg)',
                                            padding: '16px',
                                            boxShadow: 'var(--shadow-sm)'
                                        }}
                                    >
                                        {/* Encabezado del grupo de Compra */}
                                        <div style={{ 
                                            display: 'flex', 
                                            justifyContent: 'space-between', 
                                            alignItems: 'center', 
                                            borderBottom: '1px solid var(--border-subtle)', 
                                            paddingBottom: '8px', 
                                            marginBottom: '12px'
                                        }}>
                                            <h4 className="font-bold text-sm text-indigo-600" style={{ color: 'var(--accent)' }}>
                                                Compra del {formatDate(group.date)}
                                            </h4>
                                            <Badge variant="secondary">
                                                {group.items.length} {group.items.length === 1 ? 'producto' : 'productos'}
                                            </Badge>
                                        </div>

                                        {/* Lista de productos en este grupo */}
                                        <div className="flex flex-col gap-3">
                                            {group.items.map(item => (
                                                <div
                                                    key={item.id}
                                                    className="flex items-center gap-3 opacity-75 hover:opacity-100 transition-opacity"
                                                >
                                                    {/* Mini imagen del Producto */}
                                                    {item.imageUrl && item.imageUrl.trim() ? (
                                                        <img
                                                            src={item.imageUrl}
                                                            alt={item.name}
                                                            style={{
                                                                width: '40px',
                                                                height: '40px',
                                                                objectFit: 'cover',
                                                                borderRadius: 'var(--radius-md)',
                                                                backgroundColor: 'var(--bg-elevated)',
                                                                border: '1px solid var(--border-subtle)'
                                                            }}
                                                            onError={(e) => { e.target.style.display = 'none'; }}
                                                        />
                                                    ) : (
                                                        <div style={{
                                                            width: '40px',
                                                            height: '40px',
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            justifyContent: 'center',
                                                            borderRadius: 'var(--radius-md)',
                                                            backgroundColor: 'var(--accent)',
                                                            color: 'white',
                                                            fontWeight: '700'
                                                        }}>
                                                            {item.name && item.name.charAt(0).toUpperCase()}
                                                        </div>
                                                    )}
                                                    <div className="flex-grow min-w-0">
                                                        <p className="font-medium text-sm truncate line-through" style={{ color: 'var(--text-primary)' }}>
                                                            {item.name}
                                                        </p>
                                                        <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>
                                                            {item.estimatedPrice.toFixed(2)}€ | Para: {getAssigneesLabel(item.assignedUserIds)}
                                                        </p>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>
                );
            })()}

            {/* Modal de Checkout */}
            {showCheckout && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', 
                    backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 10000, 
                    display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px'
                }}>
                    <Card style={{ width: '100%', maxWidth: '400px', backgroundColor: 'var(--bg-surface)' }}>
                        <div className="p-5 border-b border-gray-200">
                            <h3 className="text-lg font-semibold" style={{ color: 'var(--text-primary)' }}>Pagar compra</h3>
                        </div>
                        <form onSubmit={handleCheckout} className="p-5 flex flex-col gap-4">
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: 'var(--text-sm)', color: 'var(--text-primary)' }}>
                                    Importe Real del Ticket (€)
                                </label>
                                <Input 
                                    type="number"
                                    step="0.01"
                                    min="0.01"
                                    required
                                    value={checkoutAmount}
                                    onChange={(e) => setCheckoutAmount(e.target.value)}
                                    placeholder="Ej: 45.50"
                                />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: 'var(--text-sm)', color: 'var(--text-primary)' }}>
                                    ¿Quién ha pagado?
                                </label>
                                <select 
                                    className="w-full"
                                    value={checkoutPayer}
                                    onChange={(e) => setCheckoutPayer(e.target.value)}
                                    style={{
                                        padding: '10px 14px', borderRadius: 'var(--radius-md)', 
                                        border: '1px solid var(--border-default)', backgroundColor: 'var(--bg-elevated)', 
                                        color: 'var(--text-primary)', outline: 'none'
                                    }}
                                >
                                    {houseMembers.map(m => (
                                        <option key={m.userId} value={m.userId}>{m.username}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="flex gap-2 mt-4">
                                <Button 
                                    type="button" 
                                    variant="secondary" 
                                    className="flex-1"
                                    onClick={() => setShowCheckout(false)}
                                >
                                    Cancelar
                                </Button>
                                <Button 
                                    type="submit" 
                                    variant="primary" 
                                    className="flex-1"
                                    disabled={checkoutLoading || !checkoutAmount}
                                >
                                    {checkoutLoading ? 'Guardando...' : 'Confirmar'}
                                </Button>
                            </div>
                        </form>
                    </Card>
                </div>
            )}
        </div>
    );
}