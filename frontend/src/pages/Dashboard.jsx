import React from 'react';
import { useQuery } from 'react-query';
import { Link } from 'react-router-dom';
import { Package, ShieldCheck, GraduationCap, AlertTriangle, CheckCircle, TrendingUp, ArrowRight } from 'lucide-react';
import { plmApi, qlmApi, llmApi } from '../api';
import { StatCard, Card, CardHeader, Badge, LoadingState } from '../components/common';

export default function Dashboard() {
  const { data: products = [] } = useQuery('products', plmApi.getProducts, { retry: 1 });
  const { data: ncrs = [] } = useQuery('ncrs', () => qlmApi.getNcrs(), { retry: 1 });
  const { data: capas = [] } = useQuery('capas', () => qlmApi.getCapas(), { retry: 1 });
  const { data: users = [] } = useQuery('users', () => llmApi.getUsers(), { retry: 1 });
  const { data: risks = [] } = useQuery('risks', qlmApi.getRisks, { retry: 1 });

  const openNcrs = ncrs.filter(n => n.status === 'OPEN');
  const criticalNcrs = ncrs.filter(n => n.severity === 'CRITICAL' && n.status === 'OPEN');
  const openCapas = capas.filter(c => c.status !== 'CLOSED' && c.status !== 'CANCELLED');
  const highRisks = risks.filter(r => r.riskScore >= 15);

  const recentActivity = [
    ...ncrs.slice(0, 3).map(n => ({ type: 'NCR', label: n.title, status: n.status, severity: n.severity, time: n.createdAt })),
    ...capas.slice(0, 2).map(c => ({ type: 'CAPA', label: c.title, status: c.status, time: c.createdAt })),
  ].sort((a, b) => new Date(b.time) - new Date(a.time)).slice(0, 8);

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h2 className="text-xl font-semibold text-gray-900 tracking-tight">Overview</h2>
        <p className="text-sm text-gray-400 mt-1">Common Data Environment — all systems at a glance</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Products" value={products.length} subtitle="Active in PLM" icon={Package} />
        <StatCard title="Open NCRs" value={openNcrs.length} subtitle={`${criticalNcrs.length} critical`} icon={AlertTriangle} />
        <StatCard title="Open CAPAs" value={openCapas.length} subtitle="Corrective actions" icon={ShieldCheck} />
        <StatCard title="Users" value={users.length} subtitle="Registered in LLM" icon={GraduationCap} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* PLM summary */}
        <Card>
          <CardHeader title="Product Lifecycle" action={
            <Link to="/plm/products" className="text-xs text-primary-600 hover:underline flex items-center gap-1">
              View all <ArrowRight size={12} />
            </Link>
          } />
          <div className="p-4 space-y-2">
            {products.slice(0, 5).map(p => (
              <Link key={p.productId} to="/plm/products"
                className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-gray-50 transition-colors">
                <div>
                  <p className="text-sm font-medium text-gray-800">{p.name}</p>
                  <p className="text-xs text-gray-400">{p.productCode}</p>
                </div>
                <Badge status={p.status} />
              </Link>
            ))}
            {products.length === 0 && <p className="text-sm text-gray-400 text-center py-6">No products yet</p>}
          </div>
        </Card>

        {/* Quality summary */}
        <Card>
          <CardHeader title="Quality Status" action={
            <Link to="/qlm/ncr" className="text-xs text-primary-600 hover:underline flex items-center gap-1">
              View NCRs <ArrowRight size={12} />
            </Link>
          } />
          <div className="p-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Open NCRs',  value: openNcrs.length },
                { label: 'Critical',   value: criticalNcrs.length },
                { label: 'Open CAPAs', value: openCapas.length },
                { label: 'High Risks', value: highRisks.length },
              ].map(stat => (
                <div key={stat.label} className="rounded-lg border border-gray-100 p-3 text-center">
                  <p className="text-2xl font-bold text-gray-900 tracking-tight">{stat.value}</p>
                  <p className="text-xs text-gray-400 mt-1">{stat.label}</p>
                </div>
              ))}
            </div>
          </div>
        </Card>

        {/* Recent activity */}
        <Card>
          <CardHeader title="Recent Activity" />
          <div className="p-4 space-y-2">
            {recentActivity.length === 0 && <p className="text-sm text-gray-400 text-center py-6">No activity yet</p>}
            {recentActivity.map((item, i) => (
              <div key={i} className="flex items-start gap-3 py-2">
                <div className={`w-1.5 h-1.5 rounded-full mt-1.5 flex-shrink-0
                  ${item.severity === 'CRITICAL' ? 'bg-red-400' : 'bg-gray-300'}`} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-800 truncate">{item.label}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-gray-400">{item.type}</span>
                    <Badge status={item.status} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Risk heatmap preview */}
      {risks.length > 0 && (
        <Card>
          <CardHeader title="Top Risks" subtitle="Sorted by risk score" action={
            <Link to="/qlm/risks" className="text-xs text-primary-600 hover:underline flex items-center gap-1">
              View all <ArrowRight size={12} />
            </Link>
          } />
          <div className="p-4 space-y-2">
            {risks.slice(0, 5).map(r => (
              <div key={r.riskId} className="flex items-center gap-4 py-2 px-3 rounded-lg hover:bg-gray-50">
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center text-xs font-bold flex-shrink-0
                  ${r.riskScore >= 20 ? 'bg-red-50 text-red-600' :
                    r.riskScore >= 12 ? 'bg-amber-50 text-amber-600' : 'bg-gray-100 text-gray-600'}`}>
                  {r.riskScore}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{r.title}</p>
                  <p className="text-xs text-gray-400">{r.category || 'General'} · L{r.likelihood} × I{r.impact}</p>
                </div>
                <Badge status={r.status} />
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
