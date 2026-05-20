import React, { useState } from 'react';
import { X, AlertCircle, CheckCircle, Info, Loader2 } from 'lucide-react';

// ── STATUS BADGE ─────────────────────────────────────────────
const DOT_COLORS = {
  ACTIVE: 'bg-gray-900',        RELEASED: 'bg-gray-900',
  APPROVED: 'bg-gray-900',      CLOSED: 'bg-gray-900',
  COMPLETED: 'bg-gray-900',     ACTIVE_CERT: 'bg-gray-900',
  DRAFT: 'bg-gray-300',         PLANNED: 'bg-gray-300',
  OBSOLETE: 'bg-gray-300',      DEPRECATED: 'bg-gray-300',
  MINOR: 'bg-gray-300',
  ENROLLED: 'bg-gray-500',      IN_PROGRESS: 'bg-gray-500',
  UNDER_REVIEW: 'bg-gray-500',  SUBMITTED: 'bg-gray-500',
  OPEN: 'bg-amber-400',         PENDING: 'bg-amber-400',
  PENDING_CAPA: 'bg-amber-400', IDENTIFIED: 'bg-amber-400',
  MAJOR: 'bg-amber-400',        STANDARD: 'bg-gray-300',
  ENHANCED: 'bg-gray-500',
  HOLD: 'bg-red-400',           CRITICAL: 'bg-red-400',
  REJECTED: 'bg-red-400',       FAILED: 'bg-red-400',
};

export function Badge({ status, className = '' }) {
  const dot = DOT_COLORS[status] || 'bg-gray-300';
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs font-medium text-gray-500 ${className}`}>
      <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${dot}`} />
      {status?.replace(/_/g, ' ')}
    </span>
  );
}

// ── CARD ─────────────────────────────────────────────────────
export function Card({ children, className = '' }) {
  return (
    <div className={`bg-white rounded-xl border border-gray-100 ${className}`}>
      {children}
    </div>
  );
}

export function CardHeader({ title, subtitle, action }) {
  return (
    <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
      <div>
        <h3 className="text-sm font-semibold text-gray-900 tracking-tight">{title}</h3>
        {subtitle && <p className="text-xs text-gray-400 mt-0.5">{subtitle}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}

// ── BUTTON ───────────────────────────────────────────────────
export function Button({ children, variant = 'primary', size = 'md', onClick, disabled, type = 'button', className = '' }) {
  const base = 'inline-flex items-center gap-1.5 font-medium rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-40';
  const sizes = { sm: 'px-3 py-1.5 text-xs', md: 'px-4 py-2 text-sm', lg: 'px-5 py-2.5 text-sm' };
  const variants = {
    primary:   'bg-gray-900 text-white hover:bg-gray-800 focus:ring-gray-700',
    secondary: 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50 focus:ring-gray-200',
    danger:    'bg-red-600 text-white hover:bg-red-700 focus:ring-red-400',
    ghost:     'text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus:ring-gray-200',
    success:   'bg-emerald-700 text-white hover:bg-emerald-800 focus:ring-emerald-500',
  };
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`${base} ${sizes[size]} ${variants[variant]} ${className}`}>
      {children}
    </button>
  );
}

// ── INPUT ────────────────────────────────────────────────────
export function Input({ label, error, ...props }) {
  return (
    <div>
      {label && <label className="block text-xs font-medium text-gray-600 mb-1.5 tracking-wide">{label}</label>}
      <input
        className={`w-full px-3 py-2 border rounded-lg text-sm bg-white text-gray-900 placeholder-gray-300
          focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition-all
          ${error ? 'border-red-300' : 'border-gray-200'}`}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
}

export function Select({ label, error, children, ...props }) {
  return (
    <div>
      {label && <label className="block text-xs font-medium text-gray-600 mb-1.5 tracking-wide">{label}</label>}
      <select
        className={`w-full px-3 py-2 border rounded-lg text-sm bg-white text-gray-900
          focus:outline-none focus:ring-2 focus:ring-gray-900 transition-all
          ${error ? 'border-red-300' : 'border-gray-200'}`}
        {...props}>
        {children}
      </select>
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
}

export function Textarea({ label, error, ...props }) {
  return (
    <div>
      {label && <label className="block text-xs font-medium text-gray-600 mb-1.5 tracking-wide">{label}</label>}
      <textarea
        rows={3}
        className={`w-full px-3 py-2 border rounded-lg text-sm bg-white text-gray-900 placeholder-gray-300
          focus:outline-none focus:ring-2 focus:ring-gray-900 transition-all resize-none
          ${error ? 'border-red-300' : 'border-gray-200'}`}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
}

// ── MODAL ────────────────────────────────────────────────────
export function Modal({ isOpen, onClose, title, children, size = 'md' }) {
  if (!isOpen) return null;
  const widths = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' };
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/20 backdrop-blur-[2px]" onClick={onClose} />
      <div className={`relative bg-white rounded-2xl border border-gray-100 shadow-xl w-full ${widths[size]} max-h-[90vh] overflow-y-auto`}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-900 tracking-tight">{title}</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <X size={16} className="text-gray-400" />
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
    success: { bg: 'bg-emerald-50 border-emerald-100', text: 'text-emerald-900', Icon: CheckCircle, iconClass: 'text-emerald-500' },
    error:   { bg: 'bg-red-50 border-red-100',         text: 'text-red-900',     Icon: AlertCircle, iconClass: 'text-red-400' },
    info:    { bg: 'bg-gray-50 border-gray-200',        text: 'text-gray-700',    Icon: Info,        iconClass: 'text-gray-400' },
    warning: { bg: 'bg-amber-50 border-amber-100',      text: 'text-amber-900',   Icon: AlertCircle, iconClass: 'text-amber-400' },
  };
  const { bg, text, Icon, iconClass } = config[type];
  return (
    <div className={`flex items-start gap-3 p-3.5 rounded-lg border ${bg}`}>
      <Icon size={15} className={`mt-0.5 flex-shrink-0 ${iconClass}`} />
      <p className={`text-sm flex-1 leading-relaxed ${text}`}>{message}</p>
      {onClose && <button onClick={onClose} className="text-gray-300 hover:text-gray-500"><X size={14} /></button>}
    </div>
  );
}

// ── TABLE ────────────────────────────────────────────────────
export function Table({ columns, data, onRowClick, emptyMessage = 'No records found.' }) {
  if (!data?.length) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-gray-300">
        <Info size={28} className="mb-3" />
        <p className="text-sm text-gray-400">{emptyMessage}</p>
      </div>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100">
            {columns.map(col => (
              <th key={col.key} className="px-4 py-3 text-left text-[11px] font-medium text-gray-400 uppercase tracking-widest">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, i) => (
            <tr key={i}
              onClick={() => onRowClick?.(row)}
              className={`border-b border-gray-50 last:border-0 transition-colors ${onRowClick ? 'cursor-pointer hover:bg-gray-50/60' : 'hover:bg-gray-50/40'}`}>
              {columns.map(col => (
                <td key={col.key} className="px-4 py-3 text-gray-600">
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
  return <Loader2 size={size} className="animate-spin text-gray-400" />;
}

export function LoadingState() {
  return (
    <div className="flex items-center justify-center h-48">
      <Spinner size={28} />
    </div>
  );
}

// ── STAT CARD ────────────────────────────────────────────────
export function StatCard({ title, value, subtitle, icon: Icon }) {
  return (
    <Card className="p-6">
      <div className="flex items-start justify-between">
        <div className="min-w-0">
          <p className="text-[11px] font-medium text-gray-400 uppercase tracking-widest truncate">{title}</p>
          <p className="mt-2 text-3xl font-bold text-gray-900 tracking-tight leading-none">{value}</p>
          {subtitle && <p className="mt-1.5 text-xs text-gray-400">{subtitle}</p>}
        </div>
        {Icon && <Icon size={18} className="text-gray-200 flex-shrink-0 mt-1" />}
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
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2">
      {toasts.map(t => (
        <div key={t.id} className="flex items-center gap-3 px-4 py-3 rounded-xl border border-gray-100 shadow-lg text-sm font-medium bg-gray-900 text-white transition-all">
          {t.type === 'success' ? <CheckCircle size={14} className="text-emerald-400" /> :
           t.type === 'error'   ? <AlertCircle size={14} className="text-red-400" /> :
                                  <Info size={14} className="text-gray-400" />}
          {t.message}
        </div>
      ))}
    </div>
  );
  return { toast, ToastContainer };
}
