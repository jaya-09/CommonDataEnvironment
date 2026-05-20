import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Package, ShieldCheck, GraduationCap,
  ChevronLeft, ChevronRight, Bell, BarChart2
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

  const currentLabel = NAV.flatMap(n => [n, ...(n.sub || [])]).find(n => n.to === location.pathname)?.label
    || 'Common Data Environment';

  return (
    <div className="flex h-screen bg-[#fafafa] overflow-hidden">

      {/* Sidebar */}
      <aside className={`flex flex-col bg-white border-r border-gray-100 transition-all duration-200 ${collapsed ? 'w-[60px]' : 'w-56'}`}>

        {/* Logo */}
        <div className={`flex items-center gap-3 border-b border-gray-100 flex-shrink-0 ${collapsed ? 'px-3 py-[18px] justify-center' : 'px-5 py-[18px]'}`}>
          <div className="w-7 h-7 bg-gray-900 rounded-md flex items-center justify-center flex-shrink-0">
            <span className="text-white text-[9px] font-bold tracking-widest">CDE</span>
          </div>
          {!collapsed && <span className="font-semibold text-gray-900 text-sm tracking-tight">CDE Platform</span>}
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 px-2 space-y-0.5">
          {NAV.map(item => {
            const Icon = item.icon;
            const active = isActive(item.to);
            return (
              <div key={item.label}>
                {item.sub ? (
                  <>
                    <button
                      onClick={() => !collapsed && toggle(item.label)}
                      className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors
                        ${active
                          ? 'bg-gray-100 text-gray-900 font-semibold'
                          : 'text-gray-500 font-medium hover:bg-gray-50 hover:text-gray-800'}`}>
                      <Icon size={16} className="flex-shrink-0" />
                      {!collapsed && (
                        <>
                          <span className="flex-1 text-left">{item.label}</span>
                          <ChevronRight size={13} className={`text-gray-300 transition-transform ${openSections[item.label] ? 'rotate-90' : ''}`} />
                        </>
                      )}
                    </button>
                    {!collapsed && openSections[item.label] && (
                      <div className="ml-4 mt-0.5 pl-3 border-l border-gray-100 space-y-0.5 pb-1">
                        {item.sub.map(s => (
                          <Link key={s.to} to={s.to}
                            className={`flex items-center px-2 py-1.5 rounded-md text-sm transition-colors
                              ${location.pathname === s.to
                                ? 'text-gray-900 font-semibold'
                                : 'text-gray-400 font-medium hover:text-gray-800'}`}>
                            {s.label}
                          </Link>
                        ))}
                      </div>
                    )}
                  </>
                ) : (
                  <Link to={item.to}
                    className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors
                      ${active
                        ? 'bg-gray-100 text-gray-900 font-semibold'
                        : 'text-gray-500 font-medium hover:bg-gray-50 hover:text-gray-800'}`}>
                    <Icon size={16} className="flex-shrink-0" />
                    {!collapsed && item.label}
                  </Link>
                )}
              </div>
            );
          })}
        </nav>

        {/* Collapse toggle */}
        <div className="px-2 py-3 border-t border-gray-100 flex-shrink-0">
          <button
            onClick={() => setCollapsed(c => !c)}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-gray-300 hover:text-gray-600 hover:bg-gray-50 transition-colors text-sm">
            {collapsed ? <ChevronRight size={15} /> : <><ChevronLeft size={15} /><span className="text-xs font-medium">Collapse</span></>}
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden min-w-0">

        {/* Top bar */}
        <header className="bg-white border-b border-gray-100 px-8 h-14 flex items-center justify-between flex-shrink-0">
          <p className="text-sm font-semibold text-gray-900 tracking-tight">{currentLabel}</p>
          <div className="flex items-center gap-2">
            <button className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-300 hover:text-gray-600 transition-colors">
              <Bell size={16} />
            </button>
            <div className="w-7 h-7 bg-gray-900 rounded-full flex items-center justify-center ml-1">
              <span className="text-white text-[10px] font-semibold">U</span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-8">
          {children}
        </main>
      </div>
    </div>
  );
}
