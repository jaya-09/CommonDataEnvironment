import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { Plus, ShieldAlert } from 'lucide-react';
import { qlmApi } from '../api';
import { Card, Table, Badge, Button, Modal, Input, Textarea, Select, useToast, LoadingState, Alert } from '../components/common';

// ── NCR PAGE ─────────────────────────────────────────────────
export function NcrPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [filter, setFilter] = useState('');
  const [form, setForm] = useState({ title: '', description: '', severity: 'MAJOR', productCode: '' });

  const { data: ncrs = [], isLoading } = useQuery(['ncrs', filter], () => qlmApi.getNcrs(filter || undefined));

  const createMutation = useMutation(qlmApi.createNcr, {
    onSuccess: () => { qc.invalidateQueries('ncrs'); setShowCreate(false); setForm({ title: '', description: '', severity: 'MAJOR', productCode: '' }); toast('NCR raised', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const updateMutation = useMutation(({ id, data }) => qlmApi.updateNcr(id, data), {
    onSuccess: () => { qc.invalidateQueries('ncrs'); toast('NCR updated', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'ncrNumber', label: 'NCR #' },
    { key: 'title', label: 'Title', render: r => <span className="block max-w-xs truncate">{r.title}</span> },
    { key: 'severity', label: 'Severity', render: r => <Badge status={r.severity} /> },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    { key: 'productCode', label: 'Product', render: r => r.productCode || '—' },
    { key: 'detectedAt', label: 'Detected', render: r => new Date(r.detectedAt).toLocaleDateString() },
    {
      key: 'actions', label: '', render: r => r.status === 'OPEN' ? (
        <Button size="sm" variant="ghost" onClick={e => { e.stopPropagation(); updateMutation.mutate({ id: r.ncrId, data: { status: 'UNDER_REVIEW' } }); }}>
          Review
        </Button>
      ) : null
    },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Non-Conformances</h2>
          <p className="text-sm text-gray-500">{ncrs.length} records</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />Raise NCR</Button>
      </div>

      <div className="flex gap-2">
        {['', 'OPEN', 'UNDER_REVIEW', 'PENDING_CAPA', 'CLOSED'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${filter === s ? 'bg-primary-600 text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'}`}>
            {s || 'All'}
          </button>
        ))}
      </div>

      <Card>
        <Table columns={columns} data={ncrs} emptyMessage="No non-conformances found." />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Raise Non-Conformance" size="lg">
        <div className="space-y-4">
          <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <Select label="Severity *" value={form.severity} onChange={e => setForm(f => ({ ...f, severity: e.target.value }))}>
              <option value="CRITICAL">Critical</option>
              <option value="MAJOR">Major</option>
              <option value="MINOR">Minor</option>
            </Select>
            <Input label="Product Code" placeholder="e.g. PROD-001" value={form.productCode}
              onChange={e => setForm(f => ({ ...f, productCode: e.target.value }))} />
          </div>
          <Textarea label="Description" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          {form.severity === 'CRITICAL' && <Alert type="warning" message="Critical NCRs will put the product version on HOLD via event." />}
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.title} variant={form.severity === 'CRITICAL' ? 'danger' : 'primary'}>
              {createMutation.isLoading ? 'Raising…' : 'Raise NCR'}
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── CAPA PAGE ─────────────────────────────────────────────────
export function CapaPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [filter, setFilter] = useState('');
  const [form, setForm] = useState({ title: '', correctiveAction: '', preventiveAction: '', ownerUserId: '', dueDate: '' });

  const { data: capas = [], isLoading } = useQuery(['capas', filter], () => qlmApi.getCapas(filter || undefined));

  const createMutation = useMutation(qlmApi.createCapa, {
    onSuccess: () => { qc.invalidateQueries('capas'); setShowCreate(false); toast('CAPA created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const closeMutation = useMutation(({ id }) => qlmApi.closeCapa(id, 'Verified effective'), {
    onSuccess: () => { qc.invalidateQueries('capas'); toast('CAPA closed', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'capaNumber', label: 'CAPA #' },
    { key: 'title', label: 'Title', render: r => <span className="block max-w-xs truncate">{r.title}</span> },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    { key: 'dueDate', label: 'Due', render: r => r.dueDate || '—' },
    { key: 'createdAt', label: 'Created', render: r => new Date(r.createdAt).toLocaleDateString() },
    {
      key: 'actions', label: '', render: r => r.status !== 'CLOSED' && r.status !== 'CANCELLED' ? (
        <Button size="sm" variant="success" onClick={e => { e.stopPropagation(); closeMutation.mutate({ id: r.capaId }); }}>Close</Button>
      ) : null
    },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">CAPA</h2>
          <p className="text-sm text-gray-500">Corrective & Preventive Actions</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />New CAPA</Button>
      </div>

      <div className="flex gap-2">
        {['', 'OPEN', 'IN_PROGRESS', 'CLOSED'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${filter === s ? 'bg-primary-600 text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'}`}>
            {s || 'All'}
          </button>
        ))}
      </div>

      <Card>
        <Table columns={columns} data={capas} emptyMessage="No CAPAs found." />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="New CAPA" size="lg">
        <div className="space-y-4">
          <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <Textarea label="Corrective Action" value={form.correctiveAction} onChange={e => setForm(f => ({ ...f, correctiveAction: e.target.value }))} />
          <Textarea label="Preventive Action" value={form.preventiveAction} onChange={e => setForm(f => ({ ...f, preventiveAction: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Owner User ID" placeholder="UUID" value={form.ownerUserId} onChange={e => setForm(f => ({ ...f, ownerUserId: e.target.value }))} />
            <Input label="Due Date" type="date" value={form.dueDate} onChange={e => setForm(f => ({ ...f, dueDate: e.target.value }))} />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate({ ...form, ownerUserId: form.ownerUserId || '00000000-0000-0000-0000-000000000001' })} disabled={!form.title}>
              Create CAPA
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── RISK PAGE ─────────────────────────────────────────────────
export function RiskPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ title: '', description: '', category: '', likelihood: 3, impact: 3, mitigationPlan: '' });

  const { data: risks = [], isLoading } = useQuery('risks', qlmApi.getRisks);

  const createMutation = useMutation(qlmApi.createRisk, {
    onSuccess: () => { qc.invalidateQueries('risks'); setShowCreate(false); toast('Risk logged', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const riskColor = score => score >= 20 ? 'text-red-600 bg-red-50' : score >= 12 ? 'text-orange-600 bg-orange-50' : 'text-yellow-600 bg-yellow-50';

  const columns = [
    { key: 'riskNumber', label: 'Risk #' },
    { key: 'title', label: 'Title', render: r => <span className="block max-w-xs truncate">{r.title}</span> },
    { key: 'category', label: 'Category', render: r => r.category || '—' },
    { key: 'riskScore', label: 'Score', render: r => (
      <span className={`inline-flex items-center justify-center w-8 h-8 rounded-lg text-xs font-bold ${riskColor(r.riskScore)}`}>{r.riskScore}</span>
    )},
    { key: 'likelihood', label: 'L × I', render: r => `${r.likelihood} × ${r.impact}` },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Risk Register</h2>
          <p className="text-sm text-gray-500">Sorted by risk score</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />Log Risk</Button>
      </div>

      <Card>
        <Table columns={columns} data={risks} emptyMessage="No risks logged." />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Log Risk" size="lg">
        <div className="space-y-4">
          <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <Input label="Category" placeholder="e.g. Technical, Compliance" value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Likelihood (1–5): <strong>{form.likelihood}</strong></label>
              <input type="range" min="1" max="5" value={form.likelihood} onChange={e => setForm(f => ({ ...f, likelihood: +e.target.value }))} className="w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Impact (1–5): <strong>{form.impact}</strong></label>
              <input type="range" min="1" max="5" value={form.impact} onChange={e => setForm(f => ({ ...f, impact: +e.target.value }))} className="w-full" />
            </div>
          </div>
          <div className={`text-center p-3 rounded-xl ${riskColor(form.likelihood * form.impact)}`}>
            <p className="text-sm font-medium">Risk Score</p>
            <p className="text-3xl font-bold">{form.likelihood * form.impact}</p>
          </div>
          <Textarea label="Mitigation Plan" value={form.mitigationPlan} onChange={e => setForm(f => ({ ...f, mitigationPlan: e.target.value }))} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.title}>Log Risk</Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── AUDIT PAGE ────────────────────────────────────────────────
export function AuditPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ auditType: 'INTERNAL', scope: '', scheduledDate: '', leadAuditorId: '00000000-0000-0000-0000-000000000001' });

  const { data: audits = [], isLoading } = useQuery('audits', () => qlmApi.getAudits());

  const createMutation = useMutation(qlmApi.createAudit, {
    onSuccess: () => { qc.invalidateQueries('audits'); setShowCreate(false); toast('Audit created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const completeMutation = useMutation(id => qlmApi.completeAudit(id, { summary: 'Audit completed' }), {
    onSuccess: () => { qc.invalidateQueries('audits'); toast('Audit completed', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'auditNumber', label: 'Audit #' },
    { key: 'auditType', label: 'Type', render: r => <Badge status={r.auditType} /> },
    { key: 'scope', label: 'Scope', render: r => r.scope || '—' },
    { key: 'scheduledDate', label: 'Scheduled', render: r => r.scheduledDate || '—' },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    {
      key: 'actions', label: '', render: r => r.status === 'PLANNED' || r.status === 'IN_PROGRESS' ? (
        <Button size="sm" variant="success" onClick={e => { e.stopPropagation(); completeMutation.mutate(r.auditId); }}>Complete</Button>
      ) : null
    },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Quality Audits</h2>
          <p className="text-sm text-gray-500">{audits.length} audits</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />New Audit</Button>
      </div>

      <Card>
        <Table columns={columns} data={audits} emptyMessage="No audits yet." />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="New Quality Audit">
        <div className="space-y-4">
          <Select label="Audit Type" value={form.auditType} onChange={e => setForm(f => ({ ...f, auditType: e.target.value }))}>
            <option value="INTERNAL">Internal</option>
            <option value="SUPPLIER">Supplier</option>
            <option value="REGULATORY">Regulatory</option>
          </Select>
          <Textarea label="Scope" value={form.scope} onChange={e => setForm(f => ({ ...f, scope: e.target.value }))} />
          <Input label="Scheduled Date" type="date" value={form.scheduledDate} onChange={e => setForm(f => ({ ...f, scheduledDate: e.target.value }))} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)}>Create Audit</Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── DOCUMENTS PAGE ────────────────────────────────────────────
export function DocumentsPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ docNumber: '', title: '', revision: 'A', documentHash: '' });

  const { data: docs = [], isLoading } = useQuery('documents', () => qlmApi.getDocuments());

  const createMutation = useMutation(qlmApi.createDocument, {
    onSuccess: () => { qc.invalidateQueries('documents'); setShowCreate(false); toast('Document registered', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const approveMutation = useMutation(qlmApi.approveDocument, {
    onSuccess: () => { qc.invalidateQueries('documents'); toast('Document approved', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'docNumber', label: 'Doc #' },
    { key: 'revision', label: 'Rev' },
    { key: 'title', label: 'Title', render: r => <span className="block max-w-xs truncate">{r.title}</span> },
    { key: 'approvalStatus', label: 'Status', render: r => <Badge status={r.approvalStatus} /> },
    { key: 'effectiveDate', label: 'Effective', render: r => r.effectiveDate || '—' },
    {
      key: 'actions', label: '', render: r => r.approvalStatus === 'DRAFT' || r.approvalStatus === 'UNDER_REVIEW' ? (
        <Button size="sm" variant="success" onClick={e => { e.stopPropagation(); approveMutation.mutate(r.docId); }}>Approve</Button>
      ) : null
    },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Document Control</h2>
          <p className="text-sm text-gray-500">{docs.length} controlled documents</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />Register Document</Button>
      </div>

      <Card>
        <Table columns={columns} data={docs} emptyMessage="No documents registered." />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Register Document" size="lg">
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label="Document Number *" placeholder="e.g. SOP-001" value={form.docNumber} onChange={e => setForm(f => ({ ...f, docNumber: e.target.value }))} />
            <Input label="Revision" placeholder="A" value={form.revision} onChange={e => setForm(f => ({ ...f, revision: e.target.value }))} />
          </div>
          <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <Input label="SHA-256 Hash *" placeholder="Document hash for integrity" value={form.documentHash} onChange={e => setForm(f => ({ ...f, documentHash: e.target.value }))} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.docNumber || !form.title || !form.documentHash}>Register</Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}
