import { useState } from 'react';
import api from '../api/client';
import { Card } from './ui/Card.jsx';
import { Button } from './ui/Button.jsx';
import { Input } from './ui/Input.jsx';
import { Alert } from './ui/Alert.jsx';
import { IconBanknotes } from './ui/Icons.jsx';

export default function DirectPaymentForm({ house, onClose, onSuccess, currentUserId, payment }) {
    const [senderId, setSenderId] = useState(payment ? payment.senderId : (currentUserId || ''));
    const [recipientId, setRecipientId] = useState(payment ? payment.recipientId : '');
    const [amount, setAmount] = useState(payment ? payment.amount.toString() : '');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!senderId || !recipientId || !amount) {
            setError('Todos los campos son obligatorios');
            return;
        }

        if (senderId === recipientId) {
            setError('El emisor y el receptor no pueden ser la misma persona');
            return;
        }

        const parsedAmount = parseFloat(amount);
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            setError('El importe debe ser un número positivo');
            return;
        }

        setSubmitting(true);
        try {
            if (payment) {
                await api.put(`/direct-payments/${payment.id}?userId=${currentUserId}`, {
                    senderId,
                    recipientId,
                    amount: parsedAmount,
                    houseId: house.id,
                });
            } else {
                await api.post('/direct-payments', {
                    senderId,
                    recipientId,
                    amount: parsedAmount,
                    houseId: house.id,
                });
            }
            onSuccess();
            onClose();
        } catch (err) {
            setError(err.response?.data?.message || `Error al ${payment ? 'actualizar' : 'registrar'} el pago directo`);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(0,0,0,0.6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1100,
                padding: 'var(--space-4)',
            }}
        >
            <Card padding="lg" style={{ maxWidth: '400px', width: '100%', borderTop: '4px solid var(--success)' }}>
                <h2 style={{ fontSize: 'var(--text-lg)', fontWeight: 'var(--font-bold)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <IconBanknotes style={{ color: 'var(--success)' }} /> {payment ? 'Editar Pago Directo / Bizum' : 'Registrar Pago Directo / Bizum'}
                </h2>

                {error && <Alert type="error" message={error} style={{ marginBottom: 'var(--space-3)' }} />}

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                        <label style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>
                            ¿Quién realiza el pago? (Emisor)
                        </label>
                        <select
                            value={senderId}
                            onChange={(e) => setSenderId(e.target.value)}
                            style={{
                                padding: 'var(--space-2)',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid var(--border-default)',
                                fontSize: 'var(--text-sm)',
                                backgroundColor: 'var(--bg-base)',
                                color: 'var(--text-primary)',
                                cursor: 'pointer'
                            }}
                            required
                        >
                            <option value="">-- Seleccionar emisor --</option>
                            {house.members.map((m) => (
                                <option key={m.userId} value={m.userId}>
                                    {m.username}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                        <label style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-bold)', color: 'var(--text-secondary)' }}>
                            ¿Quién recibe el pago? (Receptor)
                        </label>
                        <select
                            value={recipientId}
                            onChange={(e) => setRecipientId(e.target.value)}
                            style={{
                                padding: 'var(--space-2)',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid var(--border-default)',
                                fontSize: 'var(--text-sm)',
                                backgroundColor: 'var(--bg-base)',
                                color: 'var(--text-primary)',
                                cursor: 'pointer'
                            }}
                            required
                        >
                            <option value="">-- Seleccionar receptor --</option>
                            {house.members
                                .filter((m) => m.userId !== senderId)
                                .map((m) => (
                                    <option key={m.userId} value={m.userId}>
                                        {m.username}
                                    </option>
                                ))}
                        </select>
                    </div>

                    <Input
                        id="payment-amount"
                        label="Importe (€)"
                        type="number"
                        step="0.01"
                        min="0.01"
                        placeholder="Ej. 15.50"
                        required
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                    />

                    <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 'var(--space-2)' }}>
                        <Button variant="secondary" size="md" type="button" onClick={onClose} disabled={submitting} full>
                            Cancelar
                        </Button>
                        <Button variant="primary" size="md" type="submit" disabled={submitting} full>
                            {submitting ? (payment ? 'Guardando...' : 'Registrando...') : (payment ? 'Guardar Cambios' : 'Confirmar Bizum')}
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
}
