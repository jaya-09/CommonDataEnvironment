import React, { useState } from 'react';
import { useQuery } from 'react-query';
import {
  AlertTriangle, ShieldAlert, CheckCircle, XCircle, Clock,
  TrendingUp, ArrowRight, Activity, Layers, GitMerge
} from 'lucide-react';
import {
  ResponsiveContainer, ScatterChart, Scatter, XAxis, YAxis,
  CartesianGrid, Tooltip, Cell, BarChart, Bar, Legend
} from 'recharts';
import { analyticsApi } from '../api/analyticsApi';
import { Card, CardHeader, Badge, LoadingState, StatCard } from '../components/common';

// ── COLOUR HELPERS ────────────────────────────────────────────
const RISK_COLORS = {
  CRITICAL: { bg: 'bg-red-100',    text: 'text-red-700',    border: 'border-red-200',    hex: '#ef4444' },
  HIGH:     { bg: 'bg-orange-100', text: 'text-orange-700', border: 'border-orange-200', hex: '#f97316' },
  MEDIUM:   { bg: 'bg-yellow-100', text: 'text-yellow-700', border: 'border-yellow-200', hex: '#eab308' },
  LOW:      { bg: 'bg-green-100',  text: 'text-green-700',  border: 'border-green-200',  hex: '#22c55e' },
};

const STATUS_COLORS = {
  BLOCKED:  { bg: 'bg-red-50',     text: 'text-red-700',    dot: 'bg-red-500' },
  AT_RISK:  { bg: 'bg-orange-50',  text: 'text-orange-700', dot: 'bg-orange-500' },
  HEALTHY:  { bg: 'bg-green-50',   text: 'text-green-700',  dot: 'bg-green-500' },
};

const CHAIN_STEPS = [
  { key: 'NCR_OPEN',          label: 'NCR Open',          step: 1 },
  { key: 'CAPA_IN_PROGRESS',  label: 'CAPA Raised',       step: 2 },
  { key: 'CAPA_CLOSED',       label: 'CAPA Closed',       step: 3 },
  { key: 'TRAINING_TRIGGERED',label: 'Training Started',  step: 4 },
  { key: 'TRAINING_COMPLETE', label: 'Training Done',     step: 5 },
  { key: 'FULLY_RESOLVED',    label: 'Resolved',          step: 6 },
];

function getRiskCellColor(row, col) {
  // row = likelihood (1-5), col = impact (1-5), both 1-indexed
  const score = row * col;
  if (score >= 20) return '#fee2e2'; // red
  if (score >= 12) return '#ffedd5'; // orange
  if (score >= 6)  return '#fef9c3'; // yellow
  return '#f0fdf4';                  // green
}

function getRiskCellTextColor(row, col) {
  const score = row * col;
  if (score >= 20) return '#b91c1c';
  if (score >= 12) return '#c2410c';
  if (score >= 6)  return '#a16207';
  return '#15803d';
}

// ═══════════════════════════════════════════════════════════
// RISK HEATMAP — 5×5 matrix of Likelihood vs Impact
// ═══════════════════════════════════════════════════════════
function RiskHeatmap({ risks }) {
  const [selected, setSelected] = useState(null);

  // Build a map of (likelihood, impact) → [risks]
  const cellMap = {};
  risks.forEach(r => {
    const key = `${r.heatmapRow}-${r.heatmapCol}`;
    if (!cellMap[key]) cellMap[key] = [];
    cellMap[key].push(r);
  });

  const LABELS = ['', '1 Rare', '2 Unlikely', '3 Possible', '4 Likely', '5 Almost Certain'];
  const IMPACT = ['', '1 Negligible', '2 Minor', '3 Moderate', '4 Major', '5 Catastrophic'];

  return (
    <div className="space-y-4">
      {/* Matrix */}
      <div className="overflow-x-auto">
        <div className="inline-block min-w-full">
          {/* Column headers — Impact */}
          <div className="flex items-end mb-1 pl-28">
            <div className="flex-1 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
              Impact →
            </div>
          </div>
          <div className="flex items-end mb-1 pl-28">
            {[1,2,3,4,5].map(col => (
              <div key={col} className="w-24 text-center text-xs text-gray-500 font-medium px-1">
                {IMPACT[col]}
              </div>
            ))}
          </div>

          {/* Rows — Likelihood (5 down to 1) */}
          <div className="flex">
            {/* Row labels */}
            <div className="flex flex-col justify-between pr-2 py-1" style={{ width: '112px' }}>
              <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1 text-right">
                Likelihood ↓
              </div>
              {[5,4,3,2,1].map(row => (
                <div key={row} className="h-24 flex items-center justify-end pr-2">
                  <span className="text-xs text-gray-500 font-medium text-right leading-tight">
                    {LABELS[row]}
                  </span>
                </div>
              ))}
            </div>

            {/* Grid cells */}
            <div className="flex flex-col gap-1">
              {[5,4,3,2,1].map(row => (
                <div key={row} className="flex gap-1">
                  {[1,2,3,4,5].map(col => {
                    const key = `${row}-${col}`;
                    const cellRisks = cellMap[key] || [];
                    const isSelected = selected === key;
                    const score = row * col;
                    const bgColor = getRiskCellColor(row, col);
                    const textColor = getRiskCellTextColor(row, col);

                    return (
                      <button
                        key={col}
                        onClick={() => setSelected(isSelected ? null : key)}
                        className={`w-24 h-24 rounded-xl border-2 flex flex-col items-center justify-center gap-1 transition-all
                          ${isSelected ? 'ring-2 ring-blue-400 ring-offset-1 scale-105' : 'hover:scale-102'}
                          ${cellRisks.length > 0 ? 'cursor-pointer shadow-sm' : 'cursor-default opacity-60'}`}
                        style={{
                          backgroundColor: bgColor,
                          borderColor: cellRisks.length > 0 ? textColor + '40' : '#e5e7eb',
                        }}
                      >
                        <span className="text-lg font-bold" style={{ color: textColor }}>{score}</span>
                        {cellRisks.length > 0 && (
                          <span className="text-xs font-semibold px-2 py-0.5 rounded-full"
                            style={{ backgroundColor: textColor + '20', color: textColor }}>
                            {cellRisks.length} {cellRisks.length === 1 ? 'risk' : 'risks'}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center gap-4 text-xs text-gray-500">
        <span className="font-medium">Score = Likelihood × Impact</span>
        {[
          { label: 'Critical (≥20)', color: '#fee2e2', text: '#b91c1c' },
          { label: 'High (12–19)',   color: '#ffedd5', text: '#c2410c' },
          { label: 'Medium (6–11)', color: '#fef9c3', text: '#a16207' },
          { label: 'Low (1–5)',     color: '#f0fdf4', text: '#15803d' },
        ].map(l => (
          <div key={l.label} className="flex items-center gap-1">
            <div className="w-3 h-3 rounded" style={{ backgroundColor: l.color, border: `1px solid ${l.text}40` }} />
            <span>{l.label}</span>
          </div>
        ))}
      </div>

      {/* Selected cell detail */}
      {selected && cellMap[selected] && (
        <div className="border border-gray-200 rounded-2xl p-4 bg-gray-50 space-y-3">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-orange-500" />
            <span className="text-sm font-semibold text-gray-800">
              {cellMap[selected].length} risk{cellMap[selected].length > 1 ? 's' : ''} at
              L{selected.split('-')[0]} × I{selected.split('-')[1]}
            </span>
          </div>
          <div className="space-y-2">
            {cellMap[selected].map(r => (
              <RiskCard key={r.entryId} risk={r} compact />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── RISK CARD ────────────────────────────────────────────────
function RiskCard({ risk, compact = false }) {
  const c = RISK_COLORS[risk.riskLevel] || RISK_COLORS.LOW;
  const sourceLabel = {
    QLM_NCR:      { label: 'NCR',       icon: '⚠️' },
    QLM_RISK:     { label: 'Risk Reg',  icon: '📋' },
    LLM_CERT_GAP: { label: 'Cert Gap',  icon: '🎓' },
  }[risk.sourceService] || { label: risk.sourceService, icon: '📌' };

  return (
    <div className={`rounded-xl border p-3 ${c.bg} ${c.border} ${compact ? '' : 'space-y-2'}`}>
      <div className="flex items-start gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 font-bold text-sm ${c.bg} border ${c.border}`}
          style={{ color: c.hex }}>
          {risk.riskScore}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-800 leading-tight">{risk.riskTitle}</p>
          <div className="flex items-center gap-2 mt-1 flex-wrap">
            <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${c.bg} ${c.text} border ${c.border}`}>
              {risk.riskLevel}
            </span>
            <span className="text-xs text-gray-500">{sourceLabel.icon} {sourceLabel.label}</span>
            {risk.productCode && (
              <span className="text-xs text-gray-400 font-mono">{risk.productCode}</span>
            )}
          </div>
          {!compact && (
            <div className="mt-2 flex gap-3 text-xs text-gray-500">
              <span>Likelihood: <b>{risk.likelihood}</b></span>
              <span>Impact: <b>{risk.impact}</b></span>
              {risk.riskCategory && <span>Category: <b>{risk.riskCategory}</b></span>}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── NCR RESOLUTION CHAIN ─────────────────────────────────────
function NcrChainRow({ chain }) {
  const currentStep = CHAIN_STEPS.find(s => s.key === chain.resolutionStatus)?.step || 1;
  const severityColor = {
    CRITICAL: 'text-red-700 bg-red-50 border-red-200',
    MAJOR: 'text-orange-700 bg-orange-50 border-orange-200',
    MINOR: 'text-yellow-700 bg-yellow-50 border-yellow-200',
  }[chain.ncrSeverity] || 'text-gray-700 bg-gray-50 border-gray-200';

  return (
    <div className="bg-white border border-gray-200 rounded-2xl p-4 space-y-3">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className={`text-xs font-semibold px-2 py-1 rounded-lg border ${severityColor}`}>
            {chain.ncrSeverity}
          </span>
          <span className="text-sm font-semibold text-gray-800">{chain.ncrNumber}</span>
          {chain.productCode && (
            <span className="text-xs text-gray-400 font-mono">· {chain.productCode}</span>
          )}
        </div>
        <span className="text-xs text-gray-400 flex-shrink-0">
          {chain.daysSinceRaised}d ago
        </span>
      </div>

      {chain.ncrTitle && (
        <p className="text-xs text-gray-600">{chain.ncrTitle}</p>
      )}

      {/* Progress steps */}
      <div className="flex items-center gap-1">
        {CHAIN_STEPS.map((step, i) => {
          const done = step.step < currentStep;
          const active = step.step === currentStep;
          const future = step.step > currentStep;
          return (
            <React.Fragment key={step.key}>
              <div className={`flex flex-col items-center gap-1 flex-1`}>
                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all
                  ${done   ? 'bg-green-500 text-white' :
                    active  ? 'bg-blue-600 text-white ring-2 ring-blue-200' :
                    'bg-gray-100 text-gray-400'}`}>
                  {done ? '✓' : step.step}
                </div>
                <span className={`text-xs leading-tight text-center w-14 ${
                  active ? 'text-blue-700 font-semibold' : done ? 'text-green-600' : 'text-gray-400'}`}>
                  {step.label}
                </span>
              </div>
              {i < CHAIN_STEPS.length - 1 && (
                <div className={`flex-1 h-0.5 mb-4 ${done ? 'bg-green-400' : 'bg-gray-200'}`} />
              )}
            </React.Fragment>
          );
        })}
      </div>

      {/* Details row */}
      <div className="flex items-center gap-4 text-xs text-gray-500 pt-1 border-t border-gray-100">
        {chain.capaNumber && (
          <span>CAPA: <span className="font-medium text-gray-700">{chain.capaNumber}</span></span>
        )}
        {chain.trainingTriggered && (
          <span>Training: <span className="font-medium text-gray-700">
            {chain.certifiedCount}/{chain.enrolledUserCount} certified
          </span></span>
        )}
        {chain.courseCode && (
          <span>Course: <span className="font-mono text-gray-700">{chain.courseCode}</span></span>
        )}
        {chain.resolutionStatus === 'FULLY_RESOLVED' && (
          <span className="ml-auto flex items-center gap-1 text-green-600 font-semibold">
            <CheckCircle size={12} /> Fully resolved
          </span>
        )}
      </div>
    </div>
  );
}

// ── PRODUCT HEALTH TABLE ─────────────────────────────────────
function ProductHealthTable({ products }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-200">
            <th className="text-left text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Product</th>
            <th className="text-left text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Phase</th>
            <th className="text-center text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Critical NCRs</th>
            <th className="text-center text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Open CAPAs</th>
            <th className="text-center text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">High Risks</th>
            <th className="text-center text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Cert Ready</th>
            <th className="text-center text-xs font-semibold text-gray-500 uppercase tracking-wide px-3 py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {products.map(p => {
            const s = STATUS_COLORS[p.overallStatus] || STATUS_COLORS.HEALTHY;
            return (
              <tr key={p.versionId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                <td className="px-3 py-3">
                  <p className="font-medium text-gray-800">{p.productCode}</p>
                  <p className="text-xs text-gray-400">v{p.versionNumber}</p>
                </td>
                <td className="px-3 py-3 text-gray-600">{p.currentPhase || '—'}</td>
                <td className="px-3 py-3 text-center">
                  <span className={`font-semibold ${p.criticalNcrCount > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                    {p.criticalNcrCount}
                  </span>
                </td>
                <td className="px-3 py-3 text-center">
                  <span className={`font-semibold ${p.openCapaCount > 0 ? 'text-orange-600' : 'text-gray-400'}`}>
                    {p.openCapaCount}
                  </span>
                </td>
                <td className="px-3 py-3 text-center">
                  <span className={`font-semibold ${p.highRiskCount > 0 ? 'text-orange-600' : 'text-gray-400'}`}>
                    {p.highRiskCount}
                  </span>
                </td>
                <td className="px-3 py-3 text-center">
                  {p.phaseCertReady
                    ? <CheckCircle size={16} className="text-green-500 mx-auto" />
                    : <XCircle size={16} className="text-red-500 mx-auto" />}
                </td>
                <td className="px-3 py-3 text-center">
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${s.bg} ${s.text}`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${s.dot}`} />
                    {p.overallStatus}
                  </span>
                </td>
              </tr>
            );
          })}
          {products.length === 0 && (
            <tr><td colSpan={7} className="px-3 py-8 text-center text-gray-400 text-sm">
              No product health data yet — data populates as services publish events
            </td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// MAIN ANALYTICS PAGE
// ═══════════════════════════════════════════════════════════
export default function AnalyticsPage() {
  const [activeTab, setActiveTab] = useState('heatmap');
  const [statusFilter, setStatusFilter] = useState('');
  const [productFilter, setProductFilter] = useState('');

  const { data: dashboard, isLoading: dashLoading } = useQuery(
    'analytics-dashboard', analyticsApi.getDashboard, { retry: 1, refetchInterval: 30000 }
  );
  const { data: risks = [], isLoading: risksLoading } = useQuery(
    ['risk-matrix', productFilter], () => analyticsApi.getRiskMatrix(productFilter || undefined), { retry: 1 }
  );
  const { data: products = [], isLoading: productsLoading } = useQuery(
    ['product-health', statusFilter], () => analyticsApi.getProductHealth(statusFilter || undefined), { retry: 1 }
  );
  const { data: chains = [], isLoading: chainsLoading } = useQuery(
    'ncr-chains', () => analyticsApi.getNcrChains(), { retry: 1 }
  );
  const { data: timeline = [], isLoading: timelineLoading } = useQuery(
    ['timeline', productFilter], () => analyticsApi.getTimeline(productFilter ? { productCode: productFilter } : {}), { retry: 1 }
  );

  const d = dashboard || {};
  const openRisks = risks.filter(r => !r.resolved);
  const criticalRisks = openRisks.filter(r => r.riskLevel === 'CRITICAL');
  const highRisks = openRisks.filter(r => r.riskLevel === 'HIGH');

  // Unique product codes for filter
  const productCodes = [...new Set([
    ...risks.map(r => r.productCode).filter(Boolean),
    ...products.map(p => p.productCode).filter(Boolean),
  ])].sort();

  const TABS = [
    { key: 'heatmap',  label: 'Risk Heatmap',     icon: Layers },
    { key: 'health',   label: 'Product Health',   icon: Activity },
    { key: 'chains',   label: 'NCR→CAPA→Training',icon: GitMerge },
    { key: 'timeline', label: 'Event Timeline',   icon: Clock },
  ];

  return (
    <div className="space-y-6">

      {/* Page header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Cross-Service Analytics</h2>
          <p className="text-sm text-gray-500 mt-1">
            Read model built from PLM + QLM + LLM events — zero cross-service DB joins
          </p>
        </div>
        {/* Product filter */}
        <select
          value={productFilter}
          onChange={e => setProductFilter(e.target.value)}
          className="text-sm border border-gray-200 rounded-lg px-3 py-2 bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
        >
          <option value="">All products</option>
          {productCodes.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      {/* KPI strip */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
        <div className={`rounded-2xl border-2 p-4 text-center ${d.blockedVersions > 0 ? 'bg-red-50 border-red-200' : 'bg-gray-50 border-gray-200'}`}>
          <p className={`text-2xl font-bold ${d.blockedVersions > 0 ? 'text-red-700' : 'text-gray-400'}`}>{d.blockedVersions ?? '—'}</p>
          <p className="text-xs text-gray-500 mt-1 font-medium">Blocked Versions</p>
        </div>
        <div className={`rounded-2xl border-2 p-4 text-center ${criticalRisks.length > 0 ? 'bg-red-50 border-red-200' : 'bg-gray-50 border-gray-200'}`}>
          <p className={`text-2xl font-bold ${criticalRisks.length > 0 ? 'text-red-700' : 'text-gray-400'}`}>{criticalRisks.length}</p>
          <p className="text-xs text-gray-500 mt-1 font-medium">Critical Risks</p>
        </div>
        <div className={`rounded-2xl border-2 p-4 text-center ${highRisks.length > 0 ? 'bg-orange-50 border-orange-200' : 'bg-gray-50 border-gray-200'}`}>
          <p className={`text-2xl font-bold ${highRisks.length > 0 ? 'text-orange-700' : 'text-gray-400'}`}>{highRisks.length}</p>
          <p className="text-xs text-gray-500 mt-1 font-medium">High Risks</p>
        </div>
        <div className="rounded-2xl border-2 bg-gray-50 border-gray-200 p-4 text-center">
          <p className="text-2xl font-bold text-gray-700">{d.openNcrs ?? '—'}</p>
          <p className="text-xs text-gray-500 mt-1 font-medium">Open NCRs</p>
        </div>
        <div className={`rounded-2xl border-2 p-4 text-center ${(d.phaseGatesBlockedToday ?? 0) > 0 ? 'bg-orange-50 border-orange-200' : 'bg-green-50 border-green-200'}`}>
          <p className="text-xs text-gray-500 font-medium mb-1">Phase Gates Today</p>
          <div className="flex items-center justify-center gap-2">
            <span className="text-sm font-bold text-green-600">{d.phaseGatesPassedToday ?? '—'} pass</span>
            <span className="text-gray-300">·</span>
            <span className="text-sm font-bold text-red-600">{d.phaseGatesBlockedToday ?? '—'} block</span>
          </div>
        </div>
      </div>

      {/* Tab bar */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-0.5">
          {TABS.map(tab => {
            const Icon = tab.icon;
            return (
              <button key={tab.key} onClick={() => setActiveTab(tab.key)}
                className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors
                  ${activeTab === tab.key
                    ? 'border-blue-600 text-blue-700'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}>
                <Icon size={15} />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* ── TAB: RISK HEATMAP ─────────────────────────────── */}
      {activeTab === 'heatmap' && (
        <div className="space-y-6">
          {risksLoading ? <LoadingState /> : (
            <>
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Heatmap */}
                <div className="lg:col-span-2">
                  <Card>
                    <CardHeader
                      title="Risk Heatmap — Likelihood × Impact"
                      subtitle="Click any cell to see the risks inside it"
                    />
                    <div className="p-5">
                      <RiskHeatmap risks={openRisks} />
                    </div>
                  </Card>
                </div>

                {/* Risk breakdown by source */}
                <div className="space-y-4">
                  <Card>
                    <CardHeader title="Risks by Source" />
                    <div className="p-4 space-y-3">
                      {[
                        { key: 'QLM_NCR',      label: 'From NCRs',        icon: '⚠️' },
                        { key: 'QLM_RISK',     label: 'Risk Register',    icon: '📋' },
                        { key: 'LLM_CERT_GAP', label: 'Cert Gaps',        icon: '🎓' },
                      ].map(src => {
                        const count = openRisks.filter(r => r.sourceService === src.key).length;
                        const pct = openRisks.length > 0 ? Math.round((count / openRisks.length) * 100) : 0;
                        return (
                          <div key={src.key}>
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-sm text-gray-700">{src.icon} {src.label}</span>
                              <span className="text-sm font-semibold text-gray-800">{count}</span>
                            </div>
                            <div className="w-full bg-gray-100 rounded-full h-2">
                              <div className="h-2 rounded-full bg-blue-500 transition-all"
                                style={{ width: `${pct}%` }} />
                            </div>
                          </div>
                        );
                      })}
                      {openRisks.length === 0 && (
                        <p className="text-sm text-gray-400 text-center py-4">No open risks</p>
                      )}
                    </div>
                  </Card>

                  <Card>
                    <CardHeader title="Risks by Level" />
                    <div className="p-4 space-y-2">
                      {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(level => {
                        const c = RISK_COLORS[level];
                        const count = openRisks.filter(r => r.riskLevel === level).length;
                        return (
                          <div key={level} className={`flex items-center justify-between px-3 py-2 rounded-xl ${c.bg} border ${c.border}`}>
                            <span className={`text-sm font-medium ${c.text}`}>{level}</span>
                            <span className={`text-lg font-bold ${c.text}`}>{count}</span>
                          </div>
                        );
                      })}
                    </div>
                  </Card>
                </div>
              </div>

              {/* Risk list */}
              <Card>
                <CardHeader
                  title="All Open Risks"
                  subtitle={`${openRisks.length} risks across PLM, QLM and LLM — sorted by score`}
                />
                <div className="p-4 space-y-2 max-h-96 overflow-y-auto">
                  {openRisks.map(r => <RiskCard key={r.entryId} risk={r} />)}
                  {openRisks.length === 0 && (
                    <p className="text-sm text-gray-400 text-center py-6">
                      No open risks — they appear here as events are published from QLM and LLM
                    </p>
                  )}
                </div>
              </Card>
            </>
          )}
        </div>
      )}

      {/* ── TAB: PRODUCT HEALTH ───────────────────────────── */}
      {activeTab === 'health' && (
        <div className="space-y-4">
          {/* Status filter */}
          <div className="flex gap-2">
            {['', 'BLOCKED', 'AT_RISK', 'HEALTHY'].map(s => (
              <button key={s} onClick={() => setStatusFilter(s)}
                className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors
                  ${statusFilter === s
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
                {s || 'All'}
                {s === 'BLOCKED' && d.blockedVersions > 0 && (
                  <span className="ml-1.5 bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                    {d.blockedVersions}
                  </span>
                )}
              </button>
            ))}
          </div>

          {productsLoading ? <LoadingState /> : (
            <Card>
              <CardHeader
                title="Product Health — Cross-Service View"
                subtitle="PLM phase + QLM quality metrics + LLM certification readiness in one table"
              />
              <ProductHealthTable products={products} />
            </Card>
          )}
        </div>
      )}

      {/* ── TAB: NCR CHAINS ───────────────────────────────── */}
      {activeTab === 'chains' && (
        <div className="space-y-4">
          {chainsLoading ? <LoadingState /> : (
            <>
              <p className="text-sm text-gray-500">
                Each row shows the full journey of a quality issue: from NCR raised in QLM,
                through CAPA, to training triggered in LLM and certification achieved.
              </p>
              <div className="space-y-3">
                {chains.map(c => <NcrChainRow key={c.ncrId} chain={c} />)}
                {chains.length === 0 && (
                  <Card className="p-8 text-center">
                    <p className="text-gray-400 text-sm">
                      No NCR chains yet — chains appear here when QLM publishes NCR events
                    </p>
                  </Card>
                )}
              </div>
            </>
          )}
        </div>
      )}

      {/* ── TAB: TIMELINE ─────────────────────────────────── */}
      {activeTab === 'timeline' && (
        <div className="space-y-4">
          {timelineLoading ? <LoadingState /> : (
            <Card>
              <CardHeader
                title="Unified Event Timeline"
                subtitle="Chronological stream of events across all three services"
              />
              <div className="p-4 space-y-1 max-h-[600px] overflow-y-auto">
                {timeline.map((event, i) => {
                  const severityDot = {
                    CRITICAL: 'bg-red-500',
                    WARNING:  'bg-orange-400',
                    INFO:     'bg-blue-400',
                  }[event.severity] || 'bg-gray-300';
                  const serviceBadge = {
                    PLM: 'bg-blue-50 text-blue-700',
                    QLM: 'bg-orange-50 text-orange-700',
                    LLM: 'bg-purple-50 text-purple-700',
                  }[event.sourceService] || 'bg-gray-50 text-gray-700';

                  return (
                    <div key={event.timelineId || i}
                      className="flex items-start gap-3 py-2.5 border-b border-gray-100 last:border-0">
                      <div className="flex flex-col items-center pt-1.5">
                        <div className={`w-2 h-2 rounded-full flex-shrink-0 ${severityDot}`} />
                        {i < timeline.length - 1 && <div className="w-px h-full bg-gray-100 mt-1" />}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className={`text-xs font-semibold px-2 py-0.5 rounded-md ${serviceBadge}`}>
                            {event.sourceService}
                          </span>
                          {event.productCode && (
                            <span className="text-xs font-mono text-gray-400">{event.productCode}</span>
                          )}
                        </div>
                        <p className="text-sm text-gray-800 mt-0.5">{event.description}</p>
                        <p className="text-xs text-gray-400 mt-0.5">
                          {event.occurredAt ? new Date(event.occurredAt).toLocaleString() : ''}
                          {event.correlationId && (
                            <span className="ml-2 font-mono opacity-60">
                              corr: {event.correlationId.slice(0, 8)}…
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                  );
                })}
                {timeline.length === 0 && (
                  <p className="text-sm text-gray-400 text-center py-8">
                    No events yet — timeline populates as services publish events to Pub/Sub
                  </p>
                )}
              </div>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
