import React from 'react';
import { useQuery } from 'react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, Package, ShieldCheck, GraduationCap, AlertTriangle } from 'lucide-react';
import { plmApi, qlmApi, llmApi } from '../api';
import { Badge } from '../components/common';

function Stat({ value, label, sub, accent }) {
  return (
    <div className="flex flex-col gap-1 p-5 bg-white border border-gray-100 rounded-2xl">
      <span className={`text-3xl font-bold tracking-tight ${accent || 'text-gray-900'}`}>{value}</span>
      <span className="text-sm font-medium text-gray-700">{label}</span>
      {sub && <span className="text-xs text-gray-400">{sub}</span>}
    </div>
  );
}

function Section({ title, to, linkLabel, children }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-900 tracking-tight">{title}</h3>
        {to && (
          <Link to={to} className="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-900 transition-colors font-medium">
            {linkLabel || 'View all'} <ArrowRight size={11} />
          </Link>
        )}
      </div>
      <div className="bg-white border border-gray-100 rounded-2xl overflow-hidden">
        {children}
      </div>
    </div>
  );
}

function EmptyRow({ text }) {
  return (
    <div className="px-5 py-10 text-center text-sm text-gray-400">{text}</div>
  );
}

export default function Dashboard() {
  const { data: products = [] } = useQuery('products', plmApi.getProducts,  { retry: 1 });
  const { data: ncrs     = [] } = useQuery('ncrs',     () => qlmApi.getNcrs(), { retry: 1 });
  const { data: capas    = [] } = useQuery('capas',    () => qlmApi.getCapas(), { retry: 1 });
  const { data: users    = [] } = useQuery('users',    () => llmApi.getUsers(), { retry: 1 });
  const { data: risks    = [] } = useQuery('risks',    qlmApi.getRisks,        { retry: 1 });

  const openNcrs    = ncrs.filter(n => n.status === 'OPEN');
  const criticalNcrs = ncrs.filter(n => n.severity === 'CRITICAL' && n.status === 'OPEN');
  const openCapas   = capas.filter(c => c.status !== 'CLOSED' && c.status !== 'CANCELLED');
  const highRisks   = risks.filter(r => r.riskScore >= 15);

  const activity = [
    ...ncrs.slice(0,4).map(n => ({ type: 'NCR',  label: n.title,  status: n.status, severity: n.severity, time: n.createdAt })),
    ...capas.slice(0,3).map(c => ({ type: 'CAPA', label: c.title,  status: c.status, time: c.createdAt })),
  ].sort((a,b) => new Date(b.time) - new Date(a.time)).slice(0, 8);

  return (
    <div className="space-y-10">

      {/* ── Page heading ───────────────────────────────────── */}
      <div className="border-b border-gray-100 pb-6">
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Overview</h1>
        <p className="text-sm text-gray-400 mt-1">Common Data Environment — all systems at a glance</p>
      </div>

      {/* ── Stats row ──────────────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Stat value={products.length}   label="Products"    sub="Active in PLM" />
        <Stat
          value={openNcrs.length}
          label="Open NCRs"
          sub={criticalNcrs.length > 0 ? `${criticalNcrs.length} critical` : 'None critical'}
          accent={criticalNcrs.length > 0 ? 'text-red-600' : undefined}
        />
        <Stat value={openCapas.length}  label="Open CAPAs"  sub="Corrective actions" />
        <Stat value={users.length}      label="Users"       sub="Registered in LLM" />
      </div>

      {/* ── Two-column grid ────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Products */}
        <Section title="Products" to="/plm/products">
          {products.length === 0 ? <EmptyRow text="No products yet" /> : (
            <div className="divide-y divide-gray-50">
              {products.slice(0, 5).map(p => (
                <Link key={p.productId} to="/plm/products"
                  className="flex items-center justify-between px-5 py-3.5 hover:bg-gray-50 transition-colors group">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 group-hover:text-gray-700 truncate">{p.name}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{p.productCode}</p>
                  </div>
                  <Badge status={p.status} />
                </Link>
              ))}
            </div>
          )}
        </Section>

        {/* Recent activity */}
        <Section title="Recent Activity">
          {activity.length === 0 ? <EmptyRow text="No activity yet" /> : (
            <div className="divide-y divide-gray-50">
              {activity.map((item, i) => (
                <div key={i} className="flex items-center gap-4 px-5 py-3.5">
                  <div className={`w-1.5 h-1.5 rounded-full flex-shrink-0 mt-0.5
                    ${item.severity === 'CRITICAL' ? 'bg-red-400'
                      : item.type === 'CAPA'        ? 'bg-amber-400'
                      : 'bg-gray-300'}`} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-800 truncate font-medium">{item.label}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-xs text-gray-400">{item.type}</span>
                      <Badge status={item.status} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Section>

        {/* Quality numbers */}
        <Section title="Quality Metrics" to="/qlm/ncr" linkLabel="View NCRs">
          <div className="grid grid-cols-2 divide-x divide-y divide-gray-50">
            {[
              { label: 'Open NCRs',  value: openNcrs.length,    accent: openNcrs.length > 0 ? 'text-red-600' : 'text-gray-900' },
              { label: 'Critical',   value: criticalNcrs.length, accent: criticalNcrs.length > 0 ? 'text-red-600' : 'text-gray-900' },
              { label: 'Open CAPAs', value: openCapas.length,    accent: openCapas.length > 0 ? 'text-amber-600' : 'text-gray-900' },
              { label: 'High Risks', value: highRisks.length,    accent: highRisks.length > 0 ? 'text-amber-600' : 'text-gray-900' },
            ].map(s => (
              <div key={s.label} className="flex flex-col items-center justify-center py-7 gap-1">
                <span className={`text-3xl font-bold tracking-tight ${s.accent}`}>{s.value}</span>
                <span className="text-xs text-gray-400 font-medium">{s.label}</span>
              </div>
            ))}
          </div>
        </Section>

        {/* Top risks */}
        <Section title="Top Risks" to="/qlm/risks">
          {risks.length === 0 ? <EmptyRow text="No risks registered" /> : (
            <div className="divide-y divide-gray-50">
              {risks.slice(0, 5).map(r => (
                <div key={r.riskId} className="flex items-center gap-4 px-5 py-3.5 hover:bg-gray-50 transition-colors">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold flex-shrink-0
                    ${r.riskScore >= 20 ? 'bg-red-50 text-red-600'
                      : r.riskScore >= 12 ? 'bg-amber-50 text-amber-600'
                      : 'bg-gray-100 text-gray-600'}`}>
                    {r.riskScore}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-800 truncate">{r.title}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{r.category || 'General'} · L{r.likelihood} × I{r.impact}</p>
                  </div>
                  <Badge status={r.status} />
                </div>
              ))}
            </div>
          )}
        </Section>
      </div>
    </div>
  );
}
