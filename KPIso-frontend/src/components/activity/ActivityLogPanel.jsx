import { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/authContextValue.js';
import api from '../../api/client';
import { Card } from '../ui/Card.jsx';
import { Button } from '../ui/Button.jsx';
import { Alert } from '../ui/Alert.jsx';

/**
 * Componente autónomo y paginado para el muro de actividad.
 * Implementa la lectura controlada del backend evitando sobrecargar la UI.
 */
export default function ActivityLogPanel({ houseId }) {
    const authContext = useContext(AuthContext);
    const { token } = authContext || {};
    
    const [logs, setLogs] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const loadLogs = async (pageNumber, isInitial = false) => {
        if (!houseId) return;
        setLoading(true);
        setError(null);
        
        try {
            const response = await api.get(`/activity/house/${houseId}?page=${pageNumber}&size=20`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            
            const newLogs = response.data.content || [];
            
            setLogs(prev => isInitial ? newLogs : [...prev, ...newLogs]);
            // El objeto Page de Spring envía la bandera 'last' cuando no hay más páginas
            setHasMore(!response.data.last); 
            setPage(pageNumber);
        } catch (err) {
            console.error('Error al cargar la actividad:', err);
            setError('No se pudo cargar el historial de actividad.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadLogs(0, true);
    }, [houseId]);

    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES', { 
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' 
        });
    };

    return (
        <Card>
            <div className="p-5 border-b border-gray-200">
                <h3 className="text-lg font-semibold" style={{ color: 'var(--text-primary)' }}>
                    Actividad Reciente
                </h3>
            </div>
            
            <div className="p-5 flex flex-col gap-4">
                {error && <Alert type="error">{error}</Alert>}
                
                {logs.length === 0 && !loading && (
                    <p className="text-center text-gray-500 py-4">No hay actividad registrada aún.</p>
                )}

                <div className="space-y-4">
                    {logs.map((log) => (
                        <div key={log.id} className="flex flex-col border-l-2 border-indigo-200 pl-4 py-1">
                            <span className="text-sm text-gray-800 font-medium">
                                {log.message}
                            </span>
                            <span className="text-xs text-gray-400 mt-1">
                                {formatDate(log.createdAt)}
                            </span>
                        </div>
                    ))}
                </div>

                {hasMore && (
                    <div className="mt-4 text-center">
                        <Button 
                            variant="secondary" 
                            onClick={() => loadLogs(page + 1)}
                            disabled={loading}
                        >
                            {loading ? 'Cargando...' : 'Cargar más antiguas'}
                        </Button>
                    </div>
                )}
            </div>
        </Card>
    );
}