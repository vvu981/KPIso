/**
 * TabBar — Barra de pestañas estilizada y reutilizable
 *
 * Principio S: responsabilidad única de renderizar y gestionar tabs activos.
 * Principio I: el contrato de props es mínimo y específico.
 *
 * @param {{ id: string, label: string, icon?: string }[]} tabs
 * @param {string} activeTab
 * @param {(id: string) => void} onTabChange
 */
export function TabBar({ tabs, activeTab, onTabChange }) {
    return (
        <div className="tab-bar" role="tablist">
            {tabs.map((tab) => (
                <button
                    key={tab.id}
                    role="tab"
                    aria-selected={activeTab === tab.id}
                    className={`tab-btn ${activeTab === tab.id ? 'tab-btn-active' : ''}`}
                    onClick={() => onTabChange(tab.id)}
                >
                    {tab.icon && <span>{tab.icon}</span>}
                    {tab.label}
                </button>
            ))}
        </div>
    );
}
