import React, { useState, useRef, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bell, ChevronDown, BarChart2 } from 'lucide-react';

const NAV = [
  { label: 'Dashboard', to: '/' },
  {
    label: 'PLM',
    sub: [
      { label: 'Products',        to: '/plm/products',        desc: 'Manage product catalog' },
      { label: 'Change Requests', to: '/plm/change-requests', desc: 'Track CRs and approvals' },
      { label: 'Lifecycle Phases',to: '/plm/phases',          desc: 'Phase configuration' },
      { label: 'Bill of Materials',to: '/plm/bom',            desc: 'BOM tree management' },
      { label: 'TDP Documents',   to: '/plm/documents',       desc: 'Technical data packages' },
    ],
  },
  {
    label: 'QLM',
    sub: [
      { label: 'Non-Conformances', to: '/qlm/ncr',       desc: 'Quality defect reports' },
      { label: 'CAPA',             to: '/qlm/capa',      desc: 'Corrective actions' },
      { label: 'Quality Audits',   to: '/qlm/audits',    desc: 'Audit management' },
      { label: 'Risk Register',    to: '/qlm/risks',     desc: 'Risk tracking' },
      { label: 'Documents',        to: '/qlm/documents', desc: 'Controlled documents' },
    ],
  },
  { label: 'Analytics', to: '/analytics' },
  {
    label: 'LLM',
    sub: [
      { label: 'Users',            to: '/llm/users',           desc: 'User profiles' },
      { label: 'Enrollments',      to: '/llm/enrollments',     desc: 'Training enrollments' },
      { label: 'Phase Readiness',  to: '/llm/phase-readiness', desc: 'Certification status' },
    ],
  },
];

function Dropdown({ item, isActive }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const location = useLocation();
  const isParentActive = item.sub?.some(s => location.pathname.startsWith(s.to));

  useEffect(() => {
    const handleClickOutside = e => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(o => !o)}
        className={`flex items-center gap-1 px-1 py-2 text-sm font-medium transition-colors border-b-2 ${
          isParentActive
            ? 'text-gray-900 border-gray-900'
            : 'text-gray-500 border-transparent hover:text-gray-900'
        }`}
      >
        {item.label}
        <ChevronDown size={13} className={`transition-transform duration-150 ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute top-full left-0 mt-1 w-64 bg-white border border-gray-100 rounded-xl shadow-lg shadow-gray-100/80 z-50 py-2 overflow-hidden">
          {item.sub.map(s => (
            <Link
              key={s.to}
              to={s.to}
              onClick={() => setOpen(false)}
              className={`flex flex-col px-4 py-2.5 hover:bg-gray-50 transition-colors ${
                location.pathname === s.to ? 'bg-gray-50' : ''
              }`}
            >
              <span className={`text-sm font-medium ${location.pathname === s.to ? 'text-gray-900' : 'text-gray-700'}`}>
                {s.label}
              </span>
              <span className="text-xs text-gray-400 mt-0.5">{s.desc}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Layout({ children }) {
  const location = useLocation();

  const pageTitle = NAV
    .flatMap(n => n.sub ? n.sub : [n])
    .find(n => location.pathname === n.to || (n.to !== '/' && location.pathname.startsWith(n.to)))
    ?.label ?? 'Dashboard';

  return (
    <div className="min-h-screen bg-white">

      {/* ── Top nav bar (Medium style) ─────────────────────────────── */}
      <header className="sticky top-0 z-40 bg-white border-b border-gray-100">
        <div className="max-w-screen-xl mx-auto px-6 h-14 flex items-center justify-between gap-8">

          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 flex-shrink-0">
            <div className="w-7 h-7 bg-gray-900 rounded-lg flex items-center justify-center">
              <span className="text-white text-[9px] font-bold tracking-widest">CDE</span>
            </div>
            <span className="font-semibold text-gray-900 text-sm tracking-tight hidden sm:block">
              CDE Platform
            </span>
          </Link>

          {/* Nav items */}
          <nav className="flex items-center gap-1 flex-1">
            {NAV.map(item =>
              item.sub ? (
                <Dropdown key={item.label} item={item} />
              ) : (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`px-1 py-2 text-sm font-medium transition-colors border-b-2 ${
                    location.pathname === item.to
                      ? 'text-gray-900 border-gray-900'
                      : 'text-gray-500 border-transparent hover:text-gray-900'
                  }`}
                >
                  {item.label}
                </Link>
              )
            )}
          </nav>

          {/* Right actions */}
          <div className="flex items-center gap-2 flex-shrink-0">
            <button className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-colors">
              <Bell size={16} />
            </button>
            <div className="w-8 h-8 bg-gray-900 rounded-full flex items-center justify-center cursor-pointer hover:bg-gray-700 transition-colors ml-1">
              <span className="text-white text-xs font-semibold">S</span>
            </div>
          </div>
        </div>
      </header>

      {/* ── Page content ───────────────────────────────────────────── */}
      <main className="max-w-screen-xl mx-auto px-6 py-10">
        {children}
      </main>
    </div>
  );
}
