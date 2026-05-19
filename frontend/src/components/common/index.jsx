import React, { useState } from 'react';
import { X, AlertCircle, CheckCircle, Info, Loader2 } from 'lucide-react';

// ── STATUS BADGE ─────────────────────────────────────────────
const STATUS_COLORS = {
  ACTIVE: 'bg-green-100 text-green-800', RELEASED: 'bg-green-100 text-green-800',
  APPROVED: 'bg-green-100 text-green-800', CLOSED: 'bg-green-100 text-green-800',
  COMPLETED: 'bg-green-100 text-green-800', ACTIVE_CERT: 'bg-green-100 text-green-800',
  DRAFT: 'bg-gray-100 text-gray-700', PLANNED: 'bg-gray-100 text-gray-700',
  ENROLLED: 'bg-blue-100 text-blue-800', IN_PROGRESS: 'bg-blue-100 text-blue-800',
  UNDER_REVIEW: 'bg-blue-100 text-blue-800', SUBMITTED: 'bg-blue-100 text-blue-800',
  OPEN: 'bg-orange-100 text-orange-800', PENDING: 'bg-orange-100 text-orange-800',
  PENDING_CAPA: 'bg-orange-100 text-orange-800', IDENTIFIED: 'bg-orange-100 text-orange-800',
  HOLD: 'bg-red-100 text-red-800', CRITICAL: 'bg-red-100 text-red-800',
  REJECTED: 'bg-red-100 text-red-800', FAILED: 'bg-red-100 text-red-800',
  MAJOR: 'bg-orange-100 text-orange-800', MINOR: 'bg-yellow-100 text-yellow-800',
  OBSOLETE: 'bg-gray-200 text-gray-600', DEPRECATED: 'bg-gray-200 text-gray-600',
};

export function Badge({ status, className = '' }) {
  const color = STATUS_COLORS[status] || 'bg-gray-100 text-gray-600';
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${color} ${className}`}>
      {status?.replace(/_/g, ' ')}
    </span>
  );
}

// ── CARD ─────────────────────────────────────────────────────
export function Card({ children, className = '' }) {
  return (
    <div className={`bg-white rounded-xl border border-gray-200 shadow-sm ${className}`}>
      {children}
    </div>
  );
}

export function CardHeader({ title, subtitle, action }) {
  return (
    <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
      <div>
        <h3 className="text-base font-semibold text-gray-900">{title}</h3>
        {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}

// ── BUTTON ───────────────────────────────────────────────────
export function Button({ children, variant = 'primary', size = 'md', onClick, disabled, type = 'button', className = '' }) {
  const base = 'inline-flex items-center gap-2 font-medium rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-offset-2';
  const sizes = { sm: 'px-3 py-1.5 text-sm', md: 'px-4 py-2 text-sm', lg: 'px-6 py-3 text-base' };
  const variants = {
    primary: 'bg-primary-600 text-white hover:bg-primary-700 focus:ring-primary-500 disabled:opacity-50',
    secondary: 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 focus:ring-gray-300',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
    ghost: 'text-gray-600 hover:bg-gray-100 focus:ring-gray-300',
    success: 'bg-green-600 text-white hover:bg-green-700 focus:ring-green-500',
  };
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`${base} ${sizes[size]} ${variants[variant]} ${disabled ? 'cursor-not-allowed opacity-50' : ''} ${className}`}>
      {children}
    </button>
  );
}

// ── INPUT ────────────────────────────────────────────────────
export function Input({ label, error, ...props }) {
  return (
    <div>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <input
        className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all ${error ? 'border-red-300 bg-red-50' : 'border-gray-300'}`}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

export function Select({ label, error, children, ...props }) {
  return (
    <div>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <select
        className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white transition-all ${error ? 'border-red-300' : 'border-gray-300'}`}
        {...props}>
        {children}
      </select>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

export function Textarea({ label, error, ...props }) {
  return (
    <div>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <textarea
        rows={3}
        className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 transition-all resize-none ${error ? 'border-red-300' : 'border-gray-300'}`}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

// ── MODAL ────────────────────────────────────────────────────
export function Modal({ isOpen, onClose, title, children, size = 'md' }) {
  if (!isOpen) return null;
  const widths = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' };
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className={`relative bg-white rounded-2xl shadow-2xl w-full ${widths[size]} max-h-[90vh] overflow-y-auto`}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <X size={18} className="text-gray-500" />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}

// ── ALERT ────────────────────────────────────────────────────
export function Alert({ type = 'info', message, onClose }) {
  const config = {
    success: { bg: 'bg-green-50 border-green-200', text: 'text-green-800', Icon: CheckCircle, iconClass: 'text-green-500' },
    error: { bg: 'bg-red-50 border-red-200', text: 'text-red-800', Icon: AlertCircle, iconClass: 'text-red-500' },
    info: { bg: 'bg-blue-50 border-blue-200', text: 'text-blue-800', Icon: Info, iconClass: 'text-blue-500' },
    warning: { bg: 'bg-yellow-50 border-yellow-200', text: 'text-yellow-800', Icon: AlertCircle, iconClass: 'text-yellow-500' },
  };
  const { bg, text, Icon, iconClass } = config[type];
  return (
    <div className={`flex items-start gap-3 p-4 rounded-lg border ${bg}`}>
      <Icon size={18} className={`mt-0.5 flex-shrink-0 ${iconClass}`} />
      <p className={`text-sm flex-1 ${text}`}>{message}</p>
      {onClose && <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={16} /></button>}
    </div>
  );
}

// ── TABLE ────────────────────────────────────────────────────
export function Table({ columns, data, onRowClick, emptyMessage = 'No records found.' }) {
  if (!data?.length) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-gray-400">
        <Info size={32} className="mb-3 opacity-40" />
        <p className="text-sm">{emptyMessage}</p>
      </div>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100">
            {columns.map(col => (
              <th key={col.key} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {data.map((row, i) => (
            <tr key={i}
              onClick={() => onRowClick?.(row)}
              className={`hover:bg-gray-50 transition-colors ${onRowClick ? 'cursor-pointer' : ''}`}>
              {columns.map(col => (
                <td key={col.key} className="px-4 py-3 text-gray-700">
                  {col.render ? col.render(row) : row[col.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ── LOADING ──────────────────────────────────────────────────
export function Spinner({ size = 20 }) {
  return <Loader2 size={size} className="animate-spin text-primary-600" />;
}

export function LoadingState() {
  return (
    <div className="flex items-center justify-center h-48">
      <Spinner size={32} />
    </div>
  );
}

// ── STAT CARD ────────────────────────────────────────────────
export function StatCard({ title, value, subtitle, icon: Icon, color = 'blue' }) {
  const colors = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-green-50 text-green-600',
    orange: 'bg-orange-50 text-orange-600',
    red: 'bg-red-50 text-red-600',
    purple: 'bg-purple-50 text-purple-600',
  };
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
          {subtitle && <p className="mt-1 text-xs text-gray-400">{subtitle}</p>}
        </div>
        {Icon && (
          <div className={`p-2.5 rounded-xl ${colors[color]}`}>
            <Icon size={20} />
          </div>
        )}
      </div>
    </Card>
  );
}

// ── TOAST ────────────────────────────────────────────────────
export function useToast() {
  const [toasts, setToasts] = useState([]);
  const toast = (message, type = 'info') => {
    const id = Date.now();
    setToasts(t => [...t, { id, message, type }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4000);
  };
  const ToastContainer = () => (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map(t => (
        <div key={t.id} className={`flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg text-sm font-medium transition-all
          ${t.type === 'success' ? 'bg-green-600 text-white' :
            t.type === 'error' ? 'bg-red-600 text-white' : 'bg-gray-900 text-white'}`}>
          {t.type === 'success' ? <CheckCircle size={16} /> : t.type === 'error' ? <AlertCircle size={16} /> : <Info size={16} />}
          {t.message}
        </div>
      ))}
    </div>
  );
  return { toast, ToastContainer };
}
