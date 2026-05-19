import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Package, ShieldCheck, GraduationCap,
  ChevronLeft, ChevronRight, Settings, Bell, BarChart2
} from 'lucide-react';

const NAV = [
  { label: 'Dashboard', icon: LayoutDashboard, to: '/' },
  {
    label: 'PLM', icon: Package, to: '/plm',
    sub: [
      { label: 'Products', to: '/plm/products' },
      { label: 'Versions', to: '/plm/versions' },
      { label: 'Change Requests', to: '/plm/change-requests' },
      { label: 'Lifecycle Phases', to: '/plm/phases' },
    ]
  },
  {
    label: 'QLM', icon: ShieldCheck, to: '/qlm',
    sub: [
      { label: 'Non-Conformances', to: '/qlm/ncr' },
      { label: 'CAPA', to: '/qlm/capa' },
      { label: 'Quality Audits', to: '/qlm/audits' },
      { label: 'Risk Register', to: '/qlm/risks' },
      { label: 'Documents', to: '/qlm/documents' },
    ]
  },
  { label: 'Analytics', icon: BarChart2, to: '/analytics' },
  {
    label: 'LLM', icon: GraduationCap, to: '/llm',
    sub: [
      { label: 'Users', to: '/llm/users' },
      { label: 'Enrollments', to: '/llm/enrollments' },
      { label: 'Phase Readiness', to: '/llm/phase-readiness' },
    ]
  },
];

export default function Layout({ children }) {
  const [collapsed, setCollapsed] = useState(false);
  const [openSections, setOpenSections] = useState({ PLM: true, QLM: false, LLM: false });
  const location = useLocation();

  const toggle = label => setOpenSections(s => ({ ...s, [label]: !s[label] }));
  const isActive = to => location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <aside className={`flex flex-col bg-white border-r border-gray-200 transition-all duration-200 ${collapsed ? 'w-16' : 'w-60'}`}>
        {/* Logo */}
        <div className="flex items-center gap-3 px-4 py-4 border-b border-gray-100">
          <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <span className="text-white text-xs font-bold">CDE</span>
          </div>
          {!collapsed && <span className="font-bold text-gray-900 text-sm">CDE Platform</span>}
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-3 px-2">
          {NAV.map(item => {
            const Icon = item.icon;
            const active = isActive(item.to);
            return (
              <div key={item.label} className="mb-0.5">
                {item.sub ? (
                  <>
                    <button
                      onClick={() => !collapsed && toggle(item.label)}
                      className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors
                        ${active ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'}`}>
                      <Icon size={18} className="flex-shrink-0" />
                      {!collapsed && (
                        <>
                          <span className="flex-1 text-left">{item.label}</span>
                          <ChevronRight size={14} className={`transition-transform ${openSections[item.label] ? 'rotate-90' : ''}`} />
                        </>
                      )}
                    </button>
                    {!collapsed && openSections[item.label] && (
                      <div className="ml-3 mt-0.5 pl-4 border-l border-gray-200 space-y-0.5">
                        {item.sub.map(s => (
                          <Link key={s.to} to={s.to}
                            className={`flex items-center px-3 py-1.5 rounded-lg text-sm transition-colors
                              ${location.pathname === s.to ? 'bg-primary-50 text-primary-700 font-medium' : 'text-gray-500 hover:text-gray-800 hover:bg-gray-50'}`}>
                            {s.label}
                          </Link>
                        ))}
                      </div>
                    )}
                  </>
                ) : (
                  <Link to={item.to}
                    className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors
                      ${active ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'}`}>
                    <Icon size={18} className="flex-shrink-0" />
                    {!collapsed && item.label}
                  </Link>
                )}
              </div>
            );
          })}
        </nav>

        {/* Collapse button */}
        <div className="px-2 py-3 border-t border-gray-100">
          <button
            onClick={() => setCollapsed(c => !c)}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-gray-400 hover:bg-gray-100 transition-colors text-sm">
            {collapsed ? <ChevronRight size={16} /> : <><ChevronLeft size={16} /><span>Collapse</span></>}
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between flex-shrink-0">
          <div>
            <h1 className="text-base font-semibold text-gray-900">
              {NAV.flatMap(n => [n, ...(n.sub || [])]).find(n => n.to === location.pathname)?.label || 'Common Data Environment'}
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 transition-colors">
              <Bell size={18} />
            </button>
            <div className="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center">
              <span className="text-white text-xs font-bold">U</span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
