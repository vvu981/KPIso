import { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../../context/authContextValue.js';
import api from '../../api/client';
import { Button } from '../ui/Button.jsx';
import { Card } from '../ui/Card.jsx';
import { Badge } from '../ui/Badge.jsx';
import { Alert } from '../ui/Alert.jsx';
import { Input } from '../ui/Input.jsx';
import AIChatbox from './AIChatbox.jsx';

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
export default function ShoppingListSection({ houseId, currentUserId, onPurchaseRegistered, isReadOnly }) {
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
    const [hasSearchedExternal, setHasSearchedExternal] = useState(false);
    const [externalSearchError, setExternalSearchError] = useState(null);
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

    // Estado para el flujo de añadir sugerencia: comprobación de duplicado + cantidad
    const [pendingSuggestion, setPendingSuggestion] = useState(null); // sugerencia seleccionada
    const [showDuplicateModal, setShowDuplicateModal] = useState(false);
    const [showQuantityModal, setShowQuantityModal] = useState(false);
    const [selectedQuantity, setSelectedQuantity] = useState(1);

    // Grupos expandidos en la lista pendiente (Set de nombres de grupo normalizados)
    const [expandedGroups, setExpandedGroups] = useState(new Set());
    // openGroupDropdown: id del dropdown de asignación de grupo abierto
    const [openGroupDropdown, setOpenGroupDropdown] = useState(null);

    /**
     * Calcula el modo de asignación de un grupo de items:
     * 'all'    → Todos (assignedUserIds vacío)
     * 'single' → un único usuario (todos los items apuntan al mismo userId)
     * 'custom' → Personalizado (distinto por item)
     * Devuelve { mode, userId } donde userId solo aplica a 'single'.
     */
    const getGroupAssignmentMode = (groupItems) => {
        const allEmpty = groupItems.every(i => !i.assignedUserIds || i.assignedUserIds.length === 0);
        if (allEmpty) return { mode: 'all', userId: null };
        const firstIds = groupItems[0].assignedUserIds || [];
        if (firstIds.length === 1) {
            const singleId = firstIds[0];
            const allSame = groupItems.every(
                i => i.assignedUserIds?.length === 1 && i.assignedUserIds[0] === singleId
            );
            if (allSame) return { mode: 'single', userId: singleId };
        }
        return { mode: 'custom', userId: null };
    };

    /**
     * Aplica un modo de asignación a todos los items de un grupo via API.
     * mode: 'all' | 'single' | 'custom'
     * userId: solo requerido para mode='single'
     */
    const handleSetGroupAssignment = async (groupItems, mode, userId = null) => {
        try {
            const targetIds = groupItems.map(i => i.id);
            let assigneeIds = [];
            if (mode === 'single') {
                assigneeIds = [userId];
            }
            
            // Realizar peticiones en paralelo para actualizar cada item
            await Promise.all(
                targetIds.map(itemId => 
                    api.put(`/shopping-list/${itemId}/assignees`, assigneeIds, {
                        headers: token ? { Authorization: `Bearer ${token}` } : {}
                    })
                )
            );
            
            // Recargar la lista para reflejar los cambios
            loadShoppingList();
            setSuccess("Asignación de grupo actualizada correctamente.");
            setTimeout(() => setSuccess(null), 2500);
        } catch (err) {
            console.error("Error al actualizar asignaciones del grupo:", err);
            setError("No se pudo actualizar la asignación del grupo.");
        } finally {
            setOpenGroupDropdown(null);
        }
    };

    /** Toggle de expansión de un grupo */
    const toggleGroup = (groupKey) => {
        setExpandedGroups(prev => {
            const next = new Set(prev);
            next.has(groupKey) ? next.delete(groupKey) : next.add(groupKey);
            return next;
        });
    };

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

    // Cerrar dropdowns al hacer click fuera
    useEffect(() => {
        const handleOutsideClick = (e) => {
            if (openGroupDropdown && !e.target.closest('.group-assign-dropdown-container')) {
                setOpenGroupDropdown(null);
            }
            if (openDropdownId && !e.target.closest('.assignee-dropdown-container')) {
                setOpenDropdownId(null);
            }
            // Cerrar sugerencias al hacer click fuera
            if (showSuggestions && inputWrapperRef.current && !inputWrapperRef.current.contains(e.target)) {
                setShowSuggestions(false);
            }
        };

        document.addEventListener('click', handleOutsideClick);
        return () => {
            document.removeEventListener('click', handleOutsideClick);
        };
    }, [openGroupDropdown, openDropdownId, showSuggestions]);

    // Recalcular coordenadas del dropdown de sugerencias al cambiar tamaño de ventana o hacer scroll
    const updateDropdownCoords = () => {
        if (inputWrapperRef.current) {
            const rect = inputWrapperRef.current.getBoundingClientRect();
            setDropdownCoords({
                top: rect.bottom,
                left: rect.left,
                width: rect.width
            });
        }
    };

    useEffect(() => {
        if (showSuggestions) {
            updateDropdownCoords();
            window.addEventListener('resize', updateDropdownCoords);
            window.addEventListener('scroll', updateDropdownCoords, true);
        }
        return () => {
            window.removeEventListener('resize', updateDropdownCoords);
            window.removeEventListener('scroll', updateDropdownCoords, true);
        };
    }, [showSuggestions]);

    // Cargar datos al inicio
    useEffect(() => {
        if (houseId) {
            loadShoppingList();
        }
        return () => {
            if (searchTimeoutRef.current) {
                clearTimeout(searchTimeoutRef.current);
            }
        };
    }, [houseId]);

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
            setHasSearchedExternal(false);
            setExternalSearchError(null);
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
                setHasSearchedExternal(false); // Restablecer
                setExternalSearchError(null);
                setShowSuggestions(true); // Mostrar contenedor de sugerencias
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
     * Realiza una búsqueda forzada en la API externa de OpenFoodFacts (fallback)
     */
    const handleSearchExternal = async () => {
        if (!productInput.trim()) return;
        setSearchLoading(true);
        setError(null);
        setExternalSearchError(null);
        try {
            const response = await api.get(`/shopping-list/search?query=${encodeURIComponent(productInput.trim())}&useFallback=true`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            setSuggestions(response.data.slice(0, 8));
            setHasSearchedExternal(true);
            setShowSuggestions(true);
        } catch (err) {
            console.error('Error al buscar sugerencias externas:', err);
            const errMsg = err.response?.data || 'Error al consultar la API externa.';
            setExternalSearchError(errMsg);
            setHasSearchedExternal(true);
            setShowSuggestions(true);
        } finally {
            setSearchLoading(false);
        }
    };

    /**
     * Limpia el buscador y restablece el estado de sugerencias
     */
    const handleClearSearch = () => {
        setProductInput('');
        setSuggestions([]);
        setShowSuggestions(false);
        setSearchLoading(false);
        setHasSearchedExternal(false);
        setExternalSearchError(null);
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
                api.get(`/houses/${houseId}?userId=${currentUserId}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
            ]);
            setShoppingList(listRes.data);
            setHouseMembers(houseRes.data.members || []);
            if (houseRes.data.isReadOnly || isReadOnly) {
                setError('Has sido expulsado de esta vivienda o ha sido eliminada.');
            }
        } catch (err) {
            console.error('Error al cargar la lista de compra:', err);
            if (err.response?.status === 403 || err.response?.status === 404) {
                setError('Has sido expulsado de esta vivienda o ha sido eliminada.');
            } else {
                setError('No se pudo cargar la lista de compra. Intenta de nuevo.');
            }
        } finally {
            setLoading(false);
        }
    };

    /**
     * Intercepta el click en una sugerencia del dropdown.
     * Comprueba si el producto ya existe en la lista (pendiente);
     * si es así muestra el aviso de duplicado, si no va directo al selector de cantidad.
     */
    const handleSuggestionClick = (suggestion) => {
        setShowSuggestions(false);
        setPendingSuggestion(suggestion);
        setSelectedQuantity(1);

        const nameNorm = suggestion.name.trim().toLowerCase();
        const isDuplicate = shoppingList.pendingItems.some(
            item => item.name.trim().toLowerCase() === nameNorm
        );

        if (isDuplicate) {
            setShowDuplicateModal(true);
        } else {
            setShowQuantityModal(true);
        }
    };

    /**
     * Llamado cuando el usuario acepta el aviso de duplicado.
     * Cierra el aviso y abre el selector de cantidad.
     */
    const handleDuplicateAccepted = () => {
        setShowDuplicateModal(false);
        setShowQuantityModal(true);
    };

    /**
     * Confirma la cantidad y añade el producto (una vez por unidad solicitada).
     */
    const handleConfirmQuantity = async () => {
        if (!pendingSuggestion) return;
        setShowQuantityModal(false);
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            const addedItems = [];
            for (let i = 0; i < selectedQuantity; i++) {
                const response = await api.post('/shopping-list/add', {
                    productName: pendingSuggestion.name,
                    houseId: houseId,
                    addedById: currentUserId,
                    assignedUserIds: [],
                    manualPrice: pendingSuggestion.estimatedPrice
                }, {
                    headers: token ? { Authorization: `Bearer ${token}` } : {}
                });
                addedItems.push(response.data);
            }
            setShoppingList(prev => ({
                ...prev,
                pendingItems: [...addedItems.reverse(), ...prev.pendingItems],
                estimatedBudget: prev.estimatedBudget + addedItems.reduce((sum, item) => sum + (item.estimatedPrice || 0), 0)
            }));
            setProductInput('');
            setPendingSuggestion(null);
            const label = selectedQuantity > 1 ? `${selectedQuantity}× ${pendingSuggestion.name}` : pendingSuggestion.name;
            setSuccess(`"${label}" añadido a la lista`);
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            console.error('Error al añadir producto:', err);
            setError(err.response?.data || 'Error al añadir el producto. Intenta de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    /**
     * Añade un nuevo producto a la lista desde el formulario directo (input o manual).
     */
    const handleAddProduct = async (e, suggestion = null) => {
        e?.preventDefault();
        // Si viene de una sugerencia, usar el flujo de duplicado + cantidad
        if (suggestion) {
            handleSuggestionClick(suggestion);
            return;
        }
        const productName = manualMode ? manualName.trim() : productInput.trim();
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
                assignedUserIds: [],
                isManual: manualMode
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

    if (isReadOnly || error === 'Has sido expulsado de esta vivienda o ha sido eliminada.') {
        return (
            <div className="space-y-6">
                <Alert type="error" title="Acceso Restringido">
                    Has sido expulsado de esta vivienda o ha sido eliminada.
                </Alert>
            </div>
        );
    }

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

            {!isReadOnly && (
                /* Formulario de Añadir Producto */
                <Card>
                    <div className="p-5 border-b border-gray-200">
                        <h3 className="text-lg font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>Añadir Producto</h3>
                        {/* Selector de modo */}
                        <div className="flex bg-gray-100 dark:bg-gray-800 p-1 rounded-xl mb-5 max-w-[320px] border border-gray-200/50 dark:border-gray-700/50">
                            <button
                                type="button"
                                onClick={() => { setManualMode(false); setProductInput(''); }}
                                className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-semibold transition-all ${
                                    !manualMode
                                        ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-white shadow-sm'
                                        : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                                }`}
                                style={{ border: 'none', cursor: 'pointer' }}
                            >
                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                </svg>
                                Autocompletar
                            </button>
                            <button
                                type="button"
                                onClick={() => { setManualMode(true); setProductInput(''); setManualName(''); setManualPrice(''); }}
                                className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-semibold transition-all ${
                                    manualMode
                                        ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-white shadow-sm'
                                        : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                                }`}
                                style={{ border: 'none', cursor: 'pointer' }}
                            >
                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                                </svg>
                                Modo Manual
                            </button>
                        </div>
    
                        {!manualMode ? (
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
                                        {showSuggestions && (
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
                                                {suggestions.length > 0 ? (
                                                    suggestions.map((suggestion, idx) => (
                                                        <button
                                                            key={idx}
                                                            type="button"
                                                            onClick={(e) => {
                                                                e.preventDefault();
                                                                handleSuggestionClick(suggestion);
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
                                                    ))
                                                ) : (
                                                    <div style={{ padding: '16px', textAlign: 'center' }}>
                                                        {externalSearchError ? (
                                                            <p style={{ margin: 0, fontSize: 'var(--text-sm)', color: '#ef4444', fontWeight: 500 }}>
                                                                ⚠️ {externalSearchError}
                                                            </p>
                                                        ) : !hasSearchedExternal ? (
                                                            <>
                                                                <p style={{ margin: '0 0 12px 0', fontSize: 'var(--text-sm)', color: 'var(--text-secondary)' }}>
                                                                    No se ha encontrado el producto en el catálogo local.
                                                                </p>
                                                                <button
                                                                    type="button"
                                                                    onClick={handleSearchExternal}
                                                                    style={{
                                                                        background: 'var(--brand-primary, #4f46e5)',
                                                                        color: 'white',
                                                                        border: 'none',
                                                                        padding: '8px 14px',
                                                                        borderRadius: 'var(--radius-md, 6px)',
                                                                        cursor: 'pointer',
                                                                        fontSize: 'var(--text-xs)',
                                                                        fontWeight: 'bold',
                                                                        transition: 'opacity 0.2s'
                                                                    }}
                                                                    onMouseEnter={e => e.currentTarget.style.opacity = '0.9'}
                                                                    onMouseLeave={e => e.currentTarget.style.opacity = '1'}
                                                                >
                                                                    Buscar en Open Food Facts
                                                                </button>
                                                            </>
                                                        ) : (
                                                            <p style={{ margin: 0, fontSize: 'var(--text-sm)', color: 'var(--text-secondary)' }}>
                                                                No se encontraron resultados en la API externa tampoco.
                                                            </p>
                                                        )}
                                                    </div>
                                                )}
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
                                    <Button
                                        type="submit"
                                        disabled={loading || !productInput.trim()}
                                        className="flex items-center gap-2 whitespace-nowrap"
                                    >
                                        <IconPlus />
                                        Añadir
                                    </Button>
                                </div>
                            </form>
                        ) : (
                            <form onSubmit={handleAddProduct}>
                                <div className="flex gap-3">
                                    <div className="flex-grow">
                                        <Input
                                            type="text"
                                            placeholder="Nombre del producto manual"
                                            value={manualName}
                                            onChange={(e) => { setManualName(e.target.value); setProductInput(e.target.value); }}
                                            disabled={loading}
                                        />
                                    </div>
                                    <div style={{ width: '180px' }}>
                                        <Input
                                            type="number"
                                            step="0.01"
                                            min="0"
                                            placeholder="Precio (€) opcional"
                                            value={manualPrice}
                                            onChange={(e) => setManualPrice(e.target.value)}
                                            disabled={loading}
                                        />
                                    </div>
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
            )}



            {/* Lista de Productos Pendientes — agrupada por nombre */}
            {shoppingList.pendingItems.length > 0 ? (() => {
                // Agrupar items por nombre (case-insensitive)
                const groupMap = {};
                shoppingList.pendingItems.forEach(item => {
                    const key = item.name.trim().toLowerCase();
                    if (!groupMap[key]) groupMap[key] = { key, items: [] };
                    groupMap[key].items.push(item);
                });
                const groups = Object.values(groupMap);

                return (
                    <Card style={{ overflow: 'visible' }}>
                        <div className="p-5 border-b border-gray-200">
                            <h3 className="text-lg font-semibold" style={{ color: 'var(--text-primary)' }}>
                                Productos por Comprar ({shoppingList.pendingItems.length})
                            </h3>
                        </div>
                        <div className="divide-y divide-gray-200">
                            {groups.map(group => {
                                const rep = group.items[0]; // representante del grupo
                                const qty = group.items.length;
                                const isExpanded = expandedGroups.has(group.key);
                                const { mode: assignMode, userId: assignUserId } = getGroupAssignmentMode(group.items);
                                const totalEst = group.items.reduce((s, i) => s + (i.estimatedPrice || 0), 0);
                                const isGroupDropOpen = openGroupDropdown === group.key;

                                const assignLabel = qty === 1
                                    ? getAssigneesLabel(rep.assignedUserIds)
                                    : assignMode === 'all'
                                        ? 'Todos'
                                        : assignMode === 'single'
                                            ? (houseMembers.find(m => m.userId === assignUserId)?.username || 'Usuario')
                                            : 'Personalizado';

                                return (
                                    <div key={group.key}>
                                        {/* Fila principal del grupo */}
                                        <div className="p-4 flex items-center gap-4" style={{ background: 'var(--bg-surface)' }}>
                                            {/* Imagen */}
                                            <div className="flex-shrink-0">
                                                {rep.imageUrl && rep.imageUrl.trim() ? (
                                                    <img
                                                        src={rep.imageUrl}
                                                        alt={rep.name}
                                                        className="w-14 h-14 object-cover rounded-md bg-gray-100 border border-gray-200"
                                                        onError={e => { e.target.style.display = 'none'; }}
                                                    />
                                                ) : (
                                                    <div className="w-14 h-14 flex items-center justify-center rounded-md bg-indigo-500 text-white font-bold text-xl">
                                                        {rep.name && rep.name.charAt(0).toUpperCase()}
                                                    </div>
                                                )}
                                            </div>

                                            {/* Info del grupo */}
                                            <div className="flex-grow min-w-0">
                                                <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                                                    <div>
                                                        <p className="font-semibold text-base truncate" style={{ color: 'var(--text-primary)' }}>
                                                            {rep.name}
                                                            {qty > 1 && (
                                                                <span style={{
                                                                    marginLeft: 8, fontSize: '0.75rem', fontWeight: 700,
                                                                    background: 'var(--accent)', color: '#fff',
                                                                    borderRadius: '999px', padding: '1px 8px',
                                                                    verticalAlign: 'middle'
                                                                }}>
                                                                    ×{qty}
                                                                </span>
                                                            )}
                                                        </p>
                                                        <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
                                                            Estimado: <span className="font-semibold text-blue-600">
                                                                {qty > 1 ? `${totalEst.toFixed(2)}€ (${rep.estimatedPrice.toFixed(2)}€/ud.)` : `${rep.estimatedPrice.toFixed(2)}€`}
                                                            </span>
                                                        </p>
                                                    </div>

                                                    {/* Selector de asignación de grupo */}
                                                    <div className="relative flex items-center gap-1.5">
                                                        <span className="text-xs text-gray-500 font-medium">Para:</span>
                                                        <div className="relative group-assign-dropdown-container">
                                                            {isReadOnly ? (
                                                                <span className="text-xs font-medium text-gray-700">{assignLabel}</span>
                                                            ) : (
                                                                <button
                                                                    type="button"
                                                                    onClick={() => {
                                                                        setOpenGroupDropdown(isGroupDropOpen ? null : group.key);
                                                                        setOpenDropdownId(null);
                                                                    }}
                                                                    className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium border bg-white border-gray-300 text-gray-700 hover:bg-gray-50 focus:outline-none transition-colors"
                                                                >
                                                                    <span className="truncate max-w-[120px]">{assignLabel}</span>
                                                                    <IconChevronDown />
                                                                </button>
                                                            )}

                                                            {isGroupDropOpen && (
                                                                <>
                                                                    <div
                                                                        style={{ position: 'fixed', inset: 0, zIndex: 40 }}
                                                                        onClick={() => setOpenGroupDropdown(null)}
                                                                    />
                                                                    <div 
                                                                        className="absolute right-0 mt-1 bg-white border border-gray-200 rounded-md shadow-lg p-2 min-w-[190px] flex flex-col gap-1"
                                                                        style={{ zIndex: 1000 }}
                                                                    >
                                                                        {qty === 1 ? (
                                                                            <>
                                                                                <div className="px-2 py-1 text-[10px] uppercase tracking-wider font-semibold text-gray-400 border-b border-gray-100 mb-1">
                                                                                    Asignar a:
                                                                                </div>
                                                                                {houseMembers.map(member => {
                                                                                    const checked = !rep.assignedUserIds || rep.assignedUserIds.length === 0 || rep.assignedUserIds.includes(member.userId);
                                                                                    return (
                                                                                        <label
                                                                                            key={member.userId}
                                                                                            className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-gray-50 cursor-pointer text-xs select-none"
                                                                                        >
                                                                                            <input
                                                                                                type="checkbox"
                                                                                                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 h-3.5 w-3.5"
                                                                                                checked={checked}
                                                                                                onChange={() => handleToggleMember(rep, member.userId)}
                                                                                            />
                                                                                            <span className="text-gray-700 font-medium">{member.username}</span>
                                                                                        </label>
                                                                                    );
                                                                                })}
                                                                            </>
                                                                        ) : (
                                                                            <>
                                                                                <div className="px-2 py-1 text-[10px] uppercase tracking-wider font-semibold text-gray-400 border-b border-gray-100 mb-1">
                                                                                    Pagar por:
                                                                                </div>
                                                                                {/* Opción Todos */}
                                                                                <button
                                                                                    type="button"
                                                                                    className={`flex items-center gap-2 px-2 py-1.5 rounded text-xs font-medium hover:bg-gray-50 w-full text-left transition-colors ${
                                                                                        assignMode === 'all' ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'
                                                                                    }`}
                                                                                    onClick={() => { handleSetGroupAssignment(group.items, 'all'); setOpenGroupDropdown(null); }}
                                                                                >
                                                                                    <span style={{ width: 14, display: 'inline-block' }}>{assignMode === 'all' ? '✓' : ''}</span>
                                                                                    Todos (equitativo)
                                                                                </button>
                                                                                {/* Opción por usuario */}
                                                                                {houseMembers.map(m => (
                                                                                    <button
                                                                                        key={m.userId}
                                                                                        type="button"
                                                                                        className={`flex items-center gap-2 px-2 py-1.5 rounded text-xs font-medium hover:bg-gray-50 w-full text-left transition-colors ${
                                                                                            assignMode === 'single' && assignUserId === m.userId ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'
                                                                                        }`}
                                                                                        onClick={() => { handleSetGroupAssignment(group.items, 'single', m.userId); setOpenGroupDropdown(null); }}
                                                                                    >
                                                                                        <span style={{ width: 14, display: 'inline-block' }}>
                                                                                            {assignMode === 'single' && assignUserId === m.userId ? '✓' : ''}
                                                                                        </span>
                                                                                        {m.username}
                                                                                    </button>
                                                                                ))}
                                                                                {/* Opción Personalizado — solo visible si hay >1 item */}
                                                                                {qty > 1 && (
                                                                                    <>
                                                                                        <div className="border-t border-gray-100 my-1" />
                                                                                        <button
                                                                                            type="button"
                                                                                            className={`flex items-center gap-2 px-2 py-1.5 rounded text-xs font-medium hover:bg-gray-50 w-full text-left transition-colors ${
                                                                                                assignMode === 'custom' ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'
                                                                                            }`}
                                                                                            onClick={() => {
                                                                                                // Personalizado: simplemente expandir el grupo y cerrar dropdown
                                                                                                setExpandedGroups(prev => { const n = new Set(prev); n.add(group.key); return n; });
                                                                                                setOpenGroupDropdown(null);
                                                                                            }}
                                                                                        >
                                                                                            <span style={{ width: 14, display: 'inline-block' }}>{assignMode === 'custom' ? '✓' : ''}</span>
                                                                                            Personalizado
                                                                                        </button>
                                                                                    </>
                                                                                )}
                                                                            </>
                                                                        )}
                                                                    </div>
                                                                </>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>

                                            {/* Acciones del grupo */}
                                            <div className="flex-shrink-0 flex items-center gap-2">
                                                {/* Botón eliminar — solo si qty=1 */}
                                                {qty === 1 && !isReadOnly && (
                                                    <Button
                                                        variant="danger"
                                                        size="sm"
                                                        onClick={() => handleDeleteItem(rep.id, rep.name, true)}
                                                        disabled={loadingItemId === rep.id}
                                                        title="Eliminar"
                                                        className="flex items-center gap-1"
                                                    >
                                                        <IconTrash />
                                                    </Button>
                                                )}
                                                {/* Botón expandir si hay >1 item */}
                                                {qty > 1 && (
                                                    <button
                                                        type="button"
                                                        title={isExpanded ? 'Colapsar' : 'Ver unidades'}
                                                        onClick={() => toggleGroup(group.key)}
                                                        style={{
                                                            display: 'flex', alignItems: 'center', gap: 4,
                                                            padding: '6px 10px', borderRadius: 'var(--radius-md)',
                                                            border: '1px solid var(--border-default)',
                                                            background: isExpanded ? 'var(--bg-elevated)' : 'transparent',
                                                            color: 'var(--text-secondary)', cursor: 'pointer',
                                                            fontSize: '0.75rem', fontWeight: 600,
                                                            transition: 'background 0.15s',
                                                        }}
                                                    >
                                                        {isExpanded ? <IconChevronUp /> : <IconChevronDown />}
                                                    </button>
                                                )}
                                            </div>
                                        </div>

                                        {/* Subítems expandidos (modo Personalizado o manual) */}
                                        {isExpanded && qty > 1 && (
                                            <div style={{ background: 'var(--bg-base)', borderTop: '1px solid var(--border-subtle)' }}>
                                                {group.items.map((item, idx) => (
                                                    <div
                                                        key={item.id}
                                                        className="flex items-center gap-3 px-6 py-2.5"
                                                        style={{
                                                            borderBottom: idx < group.items.length - 1 ? '1px solid var(--border-subtle)' : 'none',
                                                        }}
                                                    >
                                                        {/* Número de unidad */}
                                                        <span style={{
                                                            fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-secondary)',
                                                            width: 20, textAlign: 'right', flexShrink: 0
                                                        }}>#{idx + 1}</span>

                                                        <p className="flex-grow text-sm font-medium truncate" style={{ color: 'var(--text-primary)' }}>
                                                            {item.estimatedPrice.toFixed(2)}€
                                                        </p>

                                                        {/* Asignación individual */}
                                                        <div className="relative assignee-dropdown-container">
                                                            {isReadOnly ? (
                                                                <span className="text-xs font-medium text-gray-700">{getAssigneesLabel(item.assignedUserIds)}</span>
                                                            ) : (
                                                                <button
                                                                    type="button"
                                                                    onClick={() => setOpenDropdownId(openDropdownId === item.id ? null : item.id)}
                                                                    className="flex items-center gap-1.5 px-2.5 py-1 rounded text-xs font-medium border bg-white border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors"
                                                                >
                                                                    <span>{getAssigneesLabel(item.assignedUserIds)}</span>
                                                                    <IconChevronDown />
                                                                </button>
                                                            )}
                                                            {openDropdownId === item.id && (
                                                                <>
                                                                    <div
                                                                        style={{ position: 'fixed', inset: 0, zIndex: 40 }}
                                                                        onClick={() => setOpenDropdownId(null)}
                                                                    />
                                                                    <div 
                                                                        className="absolute right-0 mt-1 bg-white border border-gray-200 rounded-md shadow-lg p-2 min-w-[160px] flex flex-col gap-1"
                                                                        style={{ zIndex: 1000 }}
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

                                                        {/* Eliminar unidad individual */}
                                                        {!isReadOnly && (
                                                            <button
                                                                type="button"
                                                                title="Eliminar esta unidad"
                                                                onClick={() => handleDeleteItem(item.id, item.name, true)}
                                                                disabled={loadingItemId === item.id}
                                                                style={{
                                                                    background: 'none', border: 'none', cursor: 'pointer',
                                                                    color: 'var(--color-danger, #ef4444)', padding: '4px',
                                                                    borderRadius: 'var(--radius-sm)', display: 'flex',
                                                                    transition: 'opacity 0.15s',
                                                                }}
                                                            >
                                                                <IconTrash />
                                                            </button>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                        {/* Footer con presupuesto y checkout */}
                        <div 
                            className="p-5 bg-gray-50 border-t border-gray-200 flex flex-col sm:flex-row justify-between items-center gap-4"
                            style={{ borderBottomLeftRadius: 'var(--radius-lg)', borderBottomRightRadius: 'var(--radius-lg)' }}
                        >
                            <div className="flex items-baseline gap-2">
                                <span className="text-sm font-medium text-gray-500">Presupuesto Estimado:</span>
                                <span className="text-2xl font-bold text-blue-600">
                                    {shoppingList.estimatedBudget.toFixed(2)}€
                                </span>
                                <span className="text-xs text-gray-400">
                                    ({shoppingList.pendingItems.length} {shoppingList.pendingItems.length === 1 ? 'producto' : 'productos'})
                                </span>
                            </div>
                            {!isReadOnly && (
                                <Button
                                    variant="primary"
                                    onClick={() => setShowCheckout(true)}
                                    className="w-full sm:w-auto px-6 py-2.5 text-sm font-semibold shadow-sm hover:shadow transition-all"
                                >
                                    Pagar compra
                                </Button>
                            )}
                        </div>
                    </Card>
                );
            })() : (
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

            {/* Modal: Aviso de Producto Duplicado */}
            {showDuplicateModal && pendingSuggestion && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
                    backgroundColor: 'rgba(0,0,0,0.55)', zIndex: 10001,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px'
                }}>
                    <Card style={{ width: '100%', maxWidth: '380px', backgroundColor: 'var(--bg-surface)' }}>
                        <div className="p-5 border-b border-gray-200 flex items-center gap-3">
                            {/* Icono de aviso */}
                            <div style={{
                                width: 36, height: 36, borderRadius: '50%',
                                backgroundColor: '#FEF3C7', display: 'flex',
                                alignItems: 'center', justifyContent: 'center', flexShrink: 0
                            }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#D97706" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                                    <line x1="12" y1="9" x2="12" y2="13"/>
                                    <line x1="12" y1="17" x2="12.01" y2="17"/>
                                </svg>
                            </div>
                            <h3 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>Producto ya en la lista</h3>
                        </div>
                        <div className="p-5">
                            <p style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)', marginBottom: '20px', lineHeight: 1.5 }}>
                                <strong style={{ color: 'var(--text-primary)' }}>«{pendingSuggestion.name}»</strong> ya está en tu lista de la compra.
                                ¿Quieres añadirlo igualmente?
                            </p>
                            <div className="flex gap-2">
                                <Button
                                    type="button"
                                    variant="secondary"
                                    className="flex-1"
                                    onClick={() => { setShowDuplicateModal(false); setPendingSuggestion(null); }}
                                >
                                    Cancelar
                                </Button>
                                <Button
                                    type="button"
                                    variant="primary"
                                    className="flex-1"
                                    onClick={handleDuplicateAccepted}
                                >
                                    Añadir de todas formas
                                </Button>
                            </div>
                        </div>
                    </Card>
                </div>
            )}

            {/* Modal: Selector de Cantidad */}
            {showQuantityModal && pendingSuggestion && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
                    backgroundColor: 'rgba(0,0,0,0.55)', zIndex: 10001,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px'
                }}>
                    <Card style={{ width: '100%', maxWidth: '380px', backgroundColor: 'var(--bg-surface)' }}>
                        <div className="p-5 border-b border-gray-200 flex items-center gap-3">
                            {pendingSuggestion.imageUrl && (
                                <img
                                    src={pendingSuggestion.imageUrl}
                                    alt={pendingSuggestion.name}
                                    style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 'var(--radius-md)', flexShrink: 0 }}
                                    onError={(e) => { e.target.style.display = 'none'; }}
                                />
                            )}
                            <div style={{ minWidth: 0 }}>
                                <h3 className="text-base font-semibold truncate" style={{ color: 'var(--text-primary)' }}>
                                    {pendingSuggestion.name}
                                </h3>
                                <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', margin: 0 }}>
                                    Est. {pendingSuggestion.estimatedPrice?.toFixed(2)}€ / ud.
                                </p>
                            </div>
                        </div>
                        <div className="p-5">
                            <p style={{ fontSize: 'var(--text-sm)', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '12px' }}>
                                ¿Cuántas unidades?
                            </p>
                            {/* Control de cantidad con botones +/- */}
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', marginBottom: '24px' }}>
                                <button
                                    type="button"
                                    onClick={() => setSelectedQuantity(q => Math.max(1, q - 1))}
                                    style={{
                                        width: 40, height: 40, borderRadius: '50%',
                                        border: '2px solid var(--border-default)',
                                        background: 'var(--bg-elevated)',
                                        color: 'var(--text-primary)',
                                        fontSize: '20px', fontWeight: 700,
                                        cursor: 'pointer', display: 'flex',
                                        alignItems: 'center', justifyContent: 'center',
                                        transition: 'background 0.15s, border-color 0.15s',
                                    }}
                                    onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                                    onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border-default)'}
                                    disabled={selectedQuantity <= 1}
                                >
                                    −
                                </button>
                                <span style={{
                                    fontSize: '2rem', fontWeight: 700,
                                    color: 'var(--text-primary)', minWidth: '3rem',
                                    textAlign: 'center', lineHeight: 1
                                }}>
                                    {selectedQuantity}
                                </span>
                                <button
                                    type="button"
                                    onClick={() => setSelectedQuantity(q => Math.min(20, q + 1))}
                                    style={{
                                        width: 40, height: 40, borderRadius: '50%',
                                        border: '2px solid var(--border-default)',
                                        background: 'var(--bg-elevated)',
                                        color: 'var(--text-primary)',
                                        fontSize: '20px', fontWeight: 700,
                                        cursor: 'pointer', display: 'flex',
                                        alignItems: 'center', justifyContent: 'center',
                                        transition: 'background 0.15s, border-color 0.15s',
                                    }}
                                    onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                                    onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border-default)'}
                                    disabled={selectedQuantity >= 20}
                                >
                                    +
                                </button>
                            </div>
                            {selectedQuantity > 1 && (
                                <p style={{ textAlign: 'center', fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', marginBottom: '16px' }}>
                                    Total est.: <strong style={{ color: 'var(--accent)' }}>
                                        {(pendingSuggestion.estimatedPrice * selectedQuantity).toFixed(2)}€
                                    </strong>
                                </p>
                            )}
                            <div className="flex gap-2">
                                <Button
                                    type="button"
                                    variant="secondary"
                                    className="flex-1"
                                    onClick={() => { setShowQuantityModal(false); setPendingSuggestion(null); }}
                                >
                                    Cancelar
                                </Button>
                                <Button
                                    type="button"
                                    variant="primary"
                                    className="flex-1"
                                    disabled={loading}
                                    onClick={handleConfirmQuantity}
                                >
                                    {loading ? 'Añadiendo...' : `Añadir ${selectedQuantity > 1 ? `(${selectedQuantity})` : ''}`}
                                </Button>
                            </div>
                        </div>
                    </Card>
                </div>
            )}

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

            {/* Chatbox Asistente IA */}
            {!isReadOnly && (
                <AIChatbox 
                    houseId={houseId} 
                    currentUserId={currentUserId} 
                    loadShoppingList={loadShoppingList} 
                />
            )}
        </div>
    );
}