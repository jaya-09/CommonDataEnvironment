import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { Plus, GitBranch, ChevronRight, ArrowRight, CheckCircle, XCircle, Loader } from 'lucide-react';
import { plmApi } from '../api';
import { Card, CardHeader, Table, Badge, Button, Modal, Input, Textarea, Select, StatCard, useToast, LoadingState, Alert } from '../components/common';

// ── PRODUCTS PAGE ─────────────────────────────────────────────
export function ProductsPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState({ productCode: '', name: '', description: '' });

  const { data: products = [], isLoading } = useQuery('products', plmApi.getProducts);
  const { data: selectedVersions = [] } = useQuery(
    ['versions', selected?.productId], () => plmApi.getVersions(selected.productId),
    { enabled: !!selected }
  );

  const createMutation = useMutation(plmApi.createProduct, {
    onSuccess: () => { qc.invalidateQueries('products'); setShowCreate(false); setForm({ productCode: '', name: '', description: '' }); toast('Product created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'productCode', label: 'Code' },
    { key: 'name', label: 'Name' },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    { key: 'createdAt', label: 'Created', render: r => new Date(r.createdAt).toLocaleDateString() },
    { key: '_', label: '', render: () => <ChevronRight size={16} className="text-gray-300" /> },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Products</h2>
          <p className="text-sm text-gray-500">{products.length} products in PLM</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />New Product</Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <Table columns={columns} data={products} onRowClick={setSelected} emptyMessage="No products yet. Create your first." />
        </Card>

        {selected && (
          <Card>
            <CardHeader title={selected.name} subtitle={selected.productCode} action={<Badge status={selected.status} />} />
            <div className="p-4">
              {selected.description && <p className="text-sm text-gray-600 mb-4">{selected.description}</p>}
              <VersionsPanel productId={selected.productId} toast={toast} />
            </div>
          </Card>
        )}
      </div>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="New Product">
        <div className="space-y-4">
          <Input label="Product Code *" placeholder="e.g. PROD-001" value={form.productCode}
            onChange={e => setForm(f => ({ ...f, productCode: e.target.value }))} />
          <Input label="Name *" placeholder="Product name" value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          <Textarea label="Description" placeholder="Optional description..." value={form.description}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.productCode || !form.name || createMutation.isLoading}>
              {createMutation.isLoading ? 'Creating…' : 'Create Product'}
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

const TERMINAL_STATUSES = new Set(['PASSED', 'BLOCKED', 'TIMEOUT']);

function VersionsPanel({ productId, toast }) {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [showPhase, setShowPhase] = useState(null);
  const [versionNumber, setVersionNumber] = useState('');
  const [phaseTarget, setPhaseTarget] = useState('');
  const [phaseResult, setPhaseResult] = useState(null);
  const [pendingGate, setPendingGate] = useState(null); // { versionId, targetPhase }

  const { data: versions = [] } = useQuery(['versions', productId], () => plmApi.getVersions(productId));
  const { data: phases = [] } = useQuery('phases', plmApi.getPhases);

  const { data: gateStatus } = useQuery(
    ['gateStatus', pendingGate?.versionId, pendingGate?.targetPhase],
    () => plmApi.getPhaseGateStatus(pendingGate.versionId, pendingGate.targetPhase),
    {
      enabled: !!pendingGate,
      refetchInterval: data => TERMINAL_STATUSES.has(data?.message) ? false : 2000,
    }
  );

  useEffect(() => {
    if (!gateStatus || !pendingGate) return;
    if (TERMINAL_STATUSES.has(gateStatus.message)) {
      setPhaseResult(gateStatus);
      setPendingGate(null);
      if (gateStatus.canAdvance) qc.invalidateQueries(['versions', productId]);
    }
  }, [gateStatus, pendingGate]);

  const closePhaseModal = () => {
    setShowPhase(null);
    setPhaseResult(null);
    setPendingGate(null);
    setPhaseTarget('');
  };

  const createMutation = useMutation(data => plmApi.createVersion(productId, data), {
    onSuccess: () => { qc.invalidateQueries(['versions', productId]); setShowCreate(false); setVersionNumber(''); toast('Version created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const phaseMutation = useMutation(({ versionId, target }) => plmApi.advancePhase(versionId, { targetPhase: target }), {
    onSuccess: (data, variables) => {
      if (data.canAdvance) {
        setPhaseResult(data);
        qc.invalidateQueries(['versions', productId]);
      } else if (data.blockReason) {
        setPhaseResult(data);
      } else {
        setPendingGate({ versionId: variables.versionId, targetPhase: variables.target });
      }
    },
    onError: e => toast(e.message, 'error'),
  });

  const isPolling = !!pendingGate;

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-semibold text-gray-700">Versions ({versions.length})</p>
        <Button size="sm" variant="secondary" onClick={() => setShowCreate(true)}><Plus size={14} />Add</Button>
      </div>
      <div className="space-y-2">
        {versions.map(v => (
          <div key={v.versionId} className="flex items-center justify-between p-3 rounded-xl bg-gray-50 hover:bg-gray-100 transition-colors">
            <div>
              <p className="text-sm font-medium text-gray-800">v{v.versionNumber}</p>
              <p className="text-xs text-gray-400">{v.currentPhase || 'No phase'}</p>
            </div>
            <div className="flex items-center gap-2">
              <Badge status={v.status} />
              <Button size="sm" variant="ghost" onClick={() => { setShowPhase(v); setPhaseResult(null); setPendingGate(null); setPhaseTarget(''); }}>
                <ArrowRight size={14} />Advance
              </Button>
            </div>
          </div>
        ))}
        {versions.length === 0 && <p className="text-xs text-gray-400 text-center py-4">No versions yet</p>}
      </div>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="New Version">
        <div className="space-y-4">
          <Input label="Version Number" placeholder="e.g. 1.0.0" value={versionNumber}
            onChange={e => setVersionNumber(e.target.value)} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate({ versionNumber })} disabled={!versionNumber}>Create</Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={!!showPhase} onClose={closePhaseModal} title="Advance Lifecycle Phase">
        {showPhase && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">Current phase: <strong>{showPhase.currentPhase || 'None'}</strong></p>
            <Select label="Target Phase" value={phaseTarget} onChange={e => setPhaseTarget(e.target.value)}
              disabled={isPolling || !!phaseResult}>
              <option value="">Select phase…</option>
              {phases.map(p => <option key={p.phaseId} value={p.phaseName}>{p.displayName}</option>)}
            </Select>

            {isPolling && (
              <div className="flex items-center gap-2 rounded-lg bg-blue-50 border border-blue-200 px-3 py-2 text-sm text-blue-700">
                <Loader size={14} className="animate-spin flex-shrink-0" />
                <span>Running gate checks (NCR + certifications)…</span>
              </div>
            )}

            {phaseResult && !isPolling && (
              <div>
                {phaseResult.canAdvance
                  ? <Alert type="success" message={`Phase advanced to ${phaseResult.toPhase || phaseResult.targetPhase} successfully!`} />
                  : phaseResult.message === 'TIMEOUT'
                    ? <Alert type="error" message="Gate check timed out — one or more services did not respond. Please try again." />
                    : <Alert type="error" message={`Blocked: ${phaseResult.blockReason || 'Gate check failed'}`} />}
              </div>
            )}

            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={closePhaseModal}>Close</Button>
              {!isPolling && !phaseResult && (
                <Button onClick={() => phaseMutation.mutate({ versionId: showPhase.versionId, target: phaseTarget })}
                  disabled={!phaseTarget || phaseMutation.isLoading}>
                  {phaseMutation.isLoading ? 'Dispatching…' : 'Advance Phase'}
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

// ── CHANGE REQUESTS PAGE ──────────────────────────────────────
export function ChangeRequestsPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const { data: products = [] } = useQuery('products', plmApi.getProducts);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [versions, setVersions] = useState([]);
  const [selectedVersion, setSelectedVersion] = useState('');
  const [crs, setCrs] = useState([]);
  const [form, setForm] = useState({ title: '', description: '', reason: '', impactAnalysis: '', crType: 'STANDARD' });

  const loadVersions = async (productId) => {
    setSelectedProduct(productId);
    if (productId) {
      const v = await plmApi.getVersions(productId);
      setVersions(v);
    }
  };

  const loadCRs = async (versionId) => {
    setSelectedVersion(versionId);
    if (versionId) {
      const c = await plmApi.getChangeRequests(versionId);
      setCrs(c);
    }
  };

  const createMutation = useMutation(data => plmApi.createChangeRequest(selectedVersion, data), {
    onSuccess: () => { loadCRs(selectedVersion); setShowCreate(false); setForm({ title: '', description: '', reason: '', impactAnalysis: '', crType: 'STANDARD' }); toast('CR created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const submitMutation = useMutation(plmApi.submitCR, {
    onSuccess: () => { loadCRs(selectedVersion); toast('CR submitted', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const approveMutation = useMutation(({ crId, decision }) => plmApi.approveCR(crId, { decision, comments: '' }), {
    onSuccess: () => { loadCRs(selectedVersion); toast('Decision recorded', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'crNumber', label: 'CR #' },
    { key: 'title', label: 'Title', render: r => <span className="truncate max-w-xs block">{r.title}</span> },
    { key: 'crType', label: 'Type', render: r => <Badge status={r.crType} /> },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    { key: 'createdAt', label: 'Created', render: r => new Date(r.createdAt).toLocaleDateString() },
    {
      key: 'actions', label: '', render: r => (
        <div className="flex gap-1">
          {r.status === 'DRAFT' && <Button size="sm" variant="ghost" onClick={e => { e.stopPropagation(); submitMutation.mutate(r.crId); }}>Submit</Button>}
          {r.status === 'SUBMITTED' && (
            <>
              <Button size="sm" variant="success" onClick={e => { e.stopPropagation(); approveMutation.mutate({ crId: r.crId, decision: 'APPROVED' }); }}>
                <CheckCircle size={14} />Approve
              </Button>
              <Button size="sm" variant="danger" onClick={e => { e.stopPropagation(); approveMutation.mutate({ crId: r.crId, decision: 'REJECTED' }); }}>
                <XCircle size={14} />Reject
              </Button>
            </>
          )}
        </div>
      )
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Change Requests</h2>
          <p className="text-sm text-gray-500">Engineering Change Requests (ECR)</p>
        </div>
        <Button onClick={() => setShowCreate(true)} disabled={!selectedVersion}><Plus size={16} />New CR</Button>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-2 gap-4">
          <Select label="Product" value={selectedProduct} onChange={e => loadVersions(e.target.value)}>
            <option value="">Select product…</option>
            {products.map(p => <option key={p.productId} value={p.productId}>{p.name} ({p.productCode})</option>)}
          </Select>
          <Select label="Version" value={selectedVersion} onChange={e => loadCRs(e.target.value)} disabled={!versions.length}>
            <option value="">Select version…</option>
            {versions.map(v => <option key={v.versionId} value={v.versionId}>v{v.versionNumber} - {v.status}</option>)}
          </Select>
        </div>
      </Card>

      <Card>
        <Table columns={columns} data={crs} emptyMessage={selectedVersion ? 'No change requests for this version.' : 'Select a version to view change requests.'} />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="New Change Request" size="lg">
        <div className="space-y-4">
          <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <Select label="Type" value={form.crType} onChange={e => setForm(f => ({ ...f, crType: e.target.value }))}>
            <option value="STANDARD">Standard</option>
            <option value="ENHANCED">Enhanced</option>
          </Select>
          <Textarea label="Description" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <Textarea label="Reason for Change" value={form.reason} onChange={e => setForm(f => ({ ...f, reason: e.target.value }))} />
          <Textarea label="Impact Analysis" value={form.impactAnalysis} onChange={e => setForm(f => ({ ...f, impactAnalysis: e.target.value }))} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.title}>Create CR</Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── PHASES PAGE ───────────────────────────────────────────────
export function PhasesPage() {
  const { data: phases = [], isLoading } = useQuery('phases', plmApi.getPhases);
  if (isLoading) return <LoadingState />;
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Lifecycle Phases</h2>
        <p className="text-sm text-gray-500">Product lifecycle phase configuration</p>
      </div>
      <div className="flex items-center gap-2 overflow-x-auto pb-4">
        {phases.map((phase, i) => (
          <React.Fragment key={phase.phaseId}>
            <div className={`flex-shrink-0 bg-white border-2 rounded-xl px-5 py-4 text-center min-w-32 ${phase.active ? 'border-primary-200' : 'border-gray-100'}`}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold mx-auto mb-2 ${phase.active ? 'bg-primary-600 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {phase.sequenceOrder}
              </div>
              <p className="text-sm font-semibold text-gray-800">{phase.displayName}</p>
              <p className="text-xs text-gray-400 mt-1">{phase.active ? 'Active' : 'Inactive'}</p>
            </div>
            {i < phases.length - 1 && <ArrowRight size={20} className="text-gray-300 flex-shrink-0" />}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
