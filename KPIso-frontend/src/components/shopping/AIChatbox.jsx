import { useState, useEffect, useRef, useContext } from 'react';
import { AuthContext } from '../../context/authContextValue.js';
import api from '../../api/client';
import { Button } from '../ui/Button.jsx';
import { Card } from '../ui/Card.jsx';

/**
 * AIChatbox — Chatbot de Asistente de Compra IA integrado con MCP.
 * Permite sugerir productos basados en recetas o dietas específicas
 * a partir de products.json y añadirlos directamente a la lista de la compra.
 */
export default function AIChatbox({ houseId, currentUserId, loadShoppingList }) {
    const authContext = useContext(AuthContext);
    const { token } = authContext || {};
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        {
            id: 'init',
            role: 'assistant',
            content: '¡Hola! Soy tu asistente de compras inteligente. Dime qué te apetece cocinar (ej. "Pasta carbonara") o qué dieta quieres seguir (ej. "Keto" o "Vegana") y buscaré los productos idóneos en nuestro piso.',
            products: []
        }
    ]);
    const [inputMessage, setInputMessage] = useState('');
    const [loading, setLoading] = useState(false);
    
    // Almacena qué productos han sido añadidos por ID de mensaje para evitar duplicados
    const [addedMessageIds, setAddedMessageIds] = useState(new Set());
    // Selección individual de productos en las sugerencias
    const [selectedProducts, setSelectedProducts] = useState({}); // { [messageId]: [idx1, idx2...] }

    const messagesEndRef = useRef(null);

    // Auto-scroll al fondo al recibir nuevos mensajes
    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages, loading]);

    const handleSendMessage = async (textToSend) => {
        const text = textToSend || inputMessage;
        if (!text.trim()) return;

        // Añadir mensaje del usuario
        const userMsgId = Date.now().toString();
        const userMsg = { id: userMsgId, role: 'user', content: text };
        setMessages(prev => [...prev, userMsg]);
        if (!textToSend) setInputMessage('');
        
        setLoading(true);

        try {
            const response = await api.post('/shopping-list/mcp/chat', {
                message: text.trim()
            }, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });

            const replyData = response.data;
            const assistantMsgId = (Date.now() + 1).toString();
            
            // Inicializar todos los productos recibidos como seleccionados por defecto
            if (replyData.products && replyData.products.length > 0) {
                setSelectedProducts(prev => ({
                    ...prev,
                    [assistantMsgId]: replyData.products.map((_, idx) => idx)
                }));
            }

            setMessages(prev => [...prev, {
                id: assistantMsgId,
                role: 'assistant',
                content: replyData.message || 'Lo siento, no he podido procesar tu solicitud.',
                products: replyData.products || [],
                intent: replyData.intent
            }]);
        } catch (error) {
            console.error('Error enviando mensaje al MCP chat:', error);
            setMessages(prev => [...prev, {
                id: Date.now().toString(),
                role: 'assistant',
                content: 'Vaya, parece que ha ocurrido un error al conectar con el asistente de IA. Asegúrate de que el microservicio de IA esté corriendo.',
                products: []
            }]);
        } finally {
            setLoading(false);
        }
    };

    // Alternar selección de un producto de un mensaje
    const handleToggleProductSelection = (messageId, index) => {
        setSelectedProducts(prev => {
            const currentSelected = prev[messageId] || [];
            if (currentSelected.includes(index)) {
                return {
                    ...prev,
                    [messageId]: currentSelected.filter(i => i !== index)
                };
            } else {
                return {
                    ...prev,
                    [messageId]: [...currentSelected, index]
                };
            }
        });
    };

    // Añadir los productos seleccionados a la lista de la compra real
    const handleAddSelectedProducts = async (messageId, productsToAdd) => {
        if (addedMessageIds.has(messageId)) return;
        
        const selectedIndices = selectedProducts[messageId] || [];
        if (selectedIndices.length === 0) return;

        setLoading(true);
        try {
            const targetProducts = selectedIndices.map(idx => productsToAdd[idx]);
            
            // Peticiones en paralelo para añadir todos los productos seleccionados
            await Promise.all(
                targetProducts.map(product => 
                    api.post('/shopping-list/add', {
                        productName: product.name,
                        houseId: houseId,
                        addedById: currentUserId,
                        assignedUserIds: []
                    }, {
                        headers: token ? { Authorization: `Bearer ${token}` } : {}
                    })
                )
            );

            // Marcar mensaje como añadido y refrescar la lista principal de compra
            setAddedMessageIds(prev => {
                const next = new Set(prev);
                next.add(messageId);
                return next;
            });

            // Cargar de nuevo la lista para que se muestren
            if (loadShoppingList) {
                loadShoppingList();
            }

        } catch (err) {
            console.error("Error al añadir productos recomendados:", err);
            alert("No se pudieron añadir algunos productos a la lista de la compra.");
        } finally {
            setLoading(false);
        }
    };

    const quickPrompts = [
        "Sugerir receta de Tortilla de patatas",
        "Dieta keto: ¿qué puedo comprar?",
        "Ingredientes para Pasta carbonara",
        "Alimentos dieta vegana"
    ];

    return (
        <>
            {/* Botón flotante del Chatbox de IA */}
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className="fixed bottom-6 right-6 z-[150] w-14 h-14 rounded-full flex items-center justify-center text-white shadow-lg transition-all duration-300 hover:scale-110 hover:shadow-indigo-500/50"
                style={{
                    background: 'var(--gradient-brand)',
                    border: 'none',
                    cursor: 'pointer'
                }}
                title="Asistente de Compra IA"
            >
                {isOpen ? (
                    <svg className="w-6 h-6 animate-pulse" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                ) : (
                    <svg className="w-6 h-6 animate-spring" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 21l3.586-1.127 1.258-2.617M15 7.5c0-.828-.672-1.5-1.5-1.5s-1.5.672-1.5 1.5.672 1.5 1.5 1.5 1.5-.672 1.5-1.5zM18 10.5c0-.828-.672-1.5-1.5-1.5s-1.5.672-1.5 1.5.672 1.5 1.5 1.5 1.5-.672 1.5-1.5zM6 10.5c0-.828-.672-1.5-1.5-1.5S3 9.672 3 10.5s.672 1.5 1.5 1.5 1.5-.672 1.5-1.5z" />
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12 5.25a.75.75 0 01.75.75v1.5a.75.75 0 01-1.5 0V6a.75.75 0 01.75-.75zM12 16.5a.75.75 0 01.75.75v.75a.75.75 0 01-1.5 0v-.75a.75.75 0 01.75-.75z" />
                    </svg>
                )}
            </button>

            {/* Ventana de chat flotante */}
            {isOpen && (
                <div 
                    className="fixed bottom-24 right-6 w-96 max-w-[calc(100vw-3rem)] h-[550px] max-h-[calc(100vh-8rem)] rounded-2xl flex flex-col z-[150] shadow-2xl overflow-hidden border border-white/10 glass-bg"
                    style={{
                        backgroundColor: 'var(--glass-bg)',
                        backdropFilter: 'var(--glass-blur)',
                        WebkitBackdropFilter: 'var(--glass-blur)'
                    }}
                >
                    {/* Header */}
                    <div 
                        className="p-4 flex justify-between items-center border-b"
                        style={{ 
                            background: 'var(--gradient-card)',
                            borderColor: 'var(--border-subtle)'
                        }}
                    >
                        <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.7)] animate-pulse"></div>
                            <span className="font-semibold text-xs" style={{ color: 'var(--text-primary)' }}>KPIso Chatbot IA</span>
                            <span 
                                className="text-[9px] px-1.5 py-0.5 rounded font-medium"
                                style={{ color: 'var(--accent-light)', backgroundColor: 'var(--accent-ultra-light)' }}
                            >
                                MCP Activo
                            </span>
                        </div>
                        <button 
                            type="button"
                            onClick={() => setIsOpen(false)}
                            className="transition-colors"
                            style={{ 
                                background: 'none', 
                                border: 'none', 
                                cursor: 'pointer',
                                color: 'var(--text-secondary)'
                            }}
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>

                    {/* Messages Area */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
                        {messages.map((msg) => (
                            <div
                                key={msg.id}
                                className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}
                            >
                                <div
                                    className={`px-4 py-2.5 rounded-2xl max-w-[85%] text-xs shadow-md border ${
                                        msg.role === 'user'
                                            ? 'text-white rounded-tr-none border-transparent'
                                            : 'rounded-tl-none'
                                    }`}
                                    style={{
                                        backgroundColor: msg.role === 'user' ? 'var(--accent)' : 'var(--bg-elevated)',
                                        color: msg.role === 'user' ? '#ffffff' : 'var(--text-primary)',
                                        borderColor: msg.role === 'user' ? 'transparent' : 'var(--border-subtle)'
                                    }}
                                >
                                    <p className="whitespace-pre-line leading-relaxed">{msg.content}</p>
                                    
                                    {/* Lista de productos recomendados */}
                                    {msg.products && msg.products.length > 0 && (
                                        <div 
                                            className="mt-3 pt-3 border-t space-y-2"
                                            style={{ borderColor: 'var(--border-subtle)' }}
                                        >
                                            <p className="text-[10px] font-semibold mb-1" style={{ color: 'var(--accent-light)' }}>
                                                Productos sugeridos:
                                            </p>
                                            <div className="max-h-48 overflow-y-auto space-y-1.5 pr-1 no-scrollbar">
                                                {msg.products.map((product, idx) => {
                                                    const isChecked = (selectedProducts[msg.id] || []).includes(idx);
                                                    return (
                                                        <label
                                                            key={idx}
                                                            className="flex items-center gap-2 p-1.5 rounded-lg border transition-all cursor-pointer text-[10px] select-none"
                                                            style={{
                                                                backgroundColor: isChecked ? 'var(--accent-ultra-light)' : 'var(--bg-surface)',
                                                                borderColor: isChecked ? 'var(--accent-light)' : 'var(--border-subtle)',
                                                                color: 'var(--text-primary)'
                                                            }}
                                                        >
                                                            <input
                                                                type="checkbox"
                                                                className="rounded focus:ring-indigo-500 h-3 w-3"
                                                                style={{ accentColor: 'var(--accent)' }}
                                                                checked={isChecked}
                                                                disabled={addedMessageIds.has(msg.id)}
                                                                onChange={() => handleToggleProductSelection(msg.id, idx)}
                                                            />
                                                            {product.imageUrl && (
                                                                <img
                                                                    src={product.imageUrl}
                                                                    alt={product.name}
                                                                    className="w-5 h-5 rounded object-cover bg-white"
                                                                />
                                                            )}
                                                            <div className="flex-1 min-w-0">
                                                                <p className="font-medium truncate" style={{ color: 'var(--text-primary)' }}>
                                                                    {product.name}
                                                                </p>
                                                                <p className="text-[8px] capitalize truncate" style={{ color: 'var(--text-tertiary)' }}>
                                                                    {product.mainCategory?.replace('en:', '') || 'General'}
                                                                </p>
                                                            </div>
                                                        </label>
                                                    );
                                                })}
                                            </div>

                                            <div className="pt-2 flex justify-end">
                                                <Button
                                                    variant={addedMessageIds.has(msg.id) ? 'success' : 'primary'}
                                                    size="xs"
                                                    disabled={addedMessageIds.has(msg.id) || (selectedProducts[msg.id] || []).length === 0}
                                                    onClick={() => handleAddSelectedProducts(msg.id, msg.products)}
                                                >
                                                    {addedMessageIds.has(msg.id) ? '✓ Añadidos' : `Añadir seleccionados (${(selectedProducts[msg.id] || []).length})`}
                                                </Button>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        {loading && (
                            <div 
                                className="flex items-center gap-1.5 p-3 rounded-2xl border w-24"
                                style={{ backgroundColor: 'var(--bg-elevated)', borderColor: 'var(--border-subtle)' }}
                            >
                                <span className="w-1.5 h-1.5 rounded-full animate-bounce" style={{ backgroundColor: 'var(--accent-light)', animationDelay: '0ms' }}></span>
                                <span className="w-1.5 h-1.5 rounded-full animate-bounce" style={{ backgroundColor: 'var(--accent-light)', animationDelay: '150ms' }}></span>
                                <span className="w-1.5 h-1.5 rounded-full animate-bounce" style={{ backgroundColor: 'var(--accent-light)', animationDelay: '300ms' }}></span>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Quick Prompts */}
                    {messages.length === 1 && !loading && (
                        <div 
                            className="px-4 py-2.5 space-y-1.5 border-t"
                            style={{ backgroundColor: 'var(--bg-surface-dark)', borderColor: 'var(--border-subtle)' }}
                        >
                            <p className="text-[9px] font-semibold uppercase tracking-wider mb-1" style={{ color: 'var(--text-secondary)' }}>
                                Prueba con:
                            </p>
                            <div className="flex flex-wrap gap-1.5">
                                {quickPrompts.map((prompt, idx) => (
                                    <button
                                        key={idx}
                                        type="button"
                                        onClick={() => handleSendMessage(prompt)}
                                        className="text-[9px] px-2.5 py-1 rounded-full border transition-all select-none"
                                        style={{ 
                                            cursor: 'pointer',
                                            backgroundColor: 'var(--accent-ultra-light)',
                                            borderColor: 'var(--border-accent)',
                                            color: 'var(--accent-light)'
                                        }}
                                    >
                                        {prompt}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Input Area */}
                    <form 
                        onSubmit={(e) => { e.preventDefault(); handleSendMessage(); }}
                        className="p-3 border-t flex gap-2 items-center"
                        style={{ backgroundColor: 'var(--bg-surface-dark)', borderColor: 'var(--border-subtle)' }}
                    >
                        <input
                            type="text"
                            value={inputMessage}
                            onChange={(e) => setInputMessage(e.target.value)}
                            placeholder="Receta o tipo de dieta..."
                            disabled={loading}
                            className="flex-1 rounded-xl px-3 py-1.5 text-xs focus:outline-none"
                            style={{
                                backgroundColor: 'var(--bg-surface)',
                                border: '1px solid var(--border-default)',
                                color: 'var(--text-primary)',
                                caretColor: 'var(--accent)'
                            }}
                        />
                        <button
                            type="submit"
                            disabled={loading || !inputMessage.trim()}
                            className="w-8 h-8 rounded-xl flex items-center justify-center text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            style={{ 
                                border: 'none', 
                                cursor: 'pointer',
                                backgroundColor: 'var(--accent)'
                            }}
                        >
                            <svg className="w-3.5 h-3.5 transform rotate-90" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
                            </svg>
                        </button>
                    </form>
                </div>
            )}
        </>
    );
}
