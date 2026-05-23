import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { Plus, GitBranch, ChevronRight, ArrowRight, CheckCircle, XCircle, Loader, Upload } from 'lucide-react';
import { plmApi } from '../api';
import { CR_STATUS, CR_TYPE, APPROVAL_DECISION } from '../constants/plm';
import { Card, CardHeader, Table, Badge, Button, Modal, Input, Textarea, Select, StatCard, useToast, LoadingState, Alert } from '../components/common';

// ── PRODUCTS PAGE ─────────────────────────────────────────────
export function ProductsPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState({ productCode: '', name: '', description: '', approverUserId: '' });

  const { data: products = [], isLoading } = useQuery('products', plmApi.getProducts);
  const { data: plmUsers = [] } = useQuery('plmUsers', plmApi.getPlmUsers);
  const { data: selectedVersions = [] } = useQuery(
    ['versions', selected?.productId], () => plmApi.getVersions(selected.productId),
    { enabled: !!selected }
  );

  const createMutation = useMutation(plmApi.createProduct, {
    onSuccess: () => { qc.invalidateQueries('products'); setShowCreate(false); setForm({ productCode: '', name: '', description: '', approverUserId: '' }); toast('Product created', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'productCode', label: 'Code' },
    { key: 'name', label: 'Name' },
    { key: 'approverName', label: 'Approver', render: r => r.approverName ? <span className="text-xs text-gray-600">{r.approverName}</span> : <span className="text-xs text-gray-300">Not set</span> },
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
          <Select label="Approver" value={form.approverUserId} onChange={e => setForm(f => ({ ...f, approverUserId: e.target.value }))}>
            <option value="">No approver assigned</option>
            {plmUsers.map(u => <option key={u.userId} value={u.userId}>{u.fullName} — {u.role}</option>)}
          </Select>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate({ ...form, approverUserId: form.approverUserId || undefined })} disabled={!form.productCode || !form.name || createMutation.isLoading}>
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
  const [form, setForm] = useState({ title: '', description: '', reason: '', impactAnalysis: '', crType: CR_TYPE.STANDARD });

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
    onSuccess: () => { loadCRs(selectedVersion); setShowCreate(false); setForm({ title: '', description: '', reason: '', impactAnalysis: '', crType: CR_TYPE.STANDARD }); toast('CR created', 'success'); },
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
          {r.status === CR_STATUS.DRAFT && <Button size="sm" variant="ghost" onClick={e => { e.stopPropagation(); submitMutation.mutate(r.crId); }}>Submit</Button>}
          {r.status === CR_STATUS.SUBMITTED && (
            <>
              <Button size="sm" variant="success" onClick={e => { e.stopPropagation(); approveMutation.mutate({ crId: r.crId, decision: APPROVAL_DECISION.APPROVED }); }}>
                <CheckCircle size={14} />Approve
              </Button>
              <Button size="sm" variant="danger" onClick={e => { e.stopPropagation(); approveMutation.mutate({ crId: r.crId, decision: APPROVAL_DECISION.REJECTED }); }}>
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
            <option value={CR_TYPE.STANDARD}>Standard</option>
            <option value={CR_TYPE.ENHANCED}>Enhanced</option>
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

// ── BOM PAGE ──────────────────────────────────────────────────
export function BomPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [selectedProduct, setSelectedProduct] = useState('');
  const [selectedVersion, setSelectedVersion] = useState('');
  const [versions, setVersions] = useState([]);
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState({ componentCode: '', name: '', componentType: '', parentComponentId: '', quantity: '1', unit: '', notes: '' });

  const { data: products = [] } = useQuery('products', plmApi.getProducts);
  const { data: bomTree = [] } = useQuery(
    ['bom', selectedVersion],
    () => plmApi.getBom(selectedVersion),
    { enabled: !!selectedVersion }
  );

  const loadVersions = async (productId) => {
    setSelectedProduct(productId);
    setSelectedVersion('');
    if (productId) {
      const v = await plmApi.getVersions(productId);
      setVersions(v);
    } else {
      setVersions([]);
    }
  };

  const flattenBom = (nodes, result = []) => {
    nodes.forEach(n => { result.push(n); if (n.children) flattenBom(n.children, result); });
    return result;
  };
  const allComponents = flattenBom(bomTree);

  const addMutation = useMutation(data => plmApi.addBomComponent(selectedVersion, data), {
    onSuccess: () => {
      qc.invalidateQueries(['bom', selectedVersion]);
      setShowAdd(false);
      setForm({ componentCode: '', name: '', componentType: '', parentComponentId: '', quantity: '1', unit: '', notes: '' });
      toast('Component added', 'success');
    },
    onError: e => toast(e.message, 'error'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Bill of Materials</h2>
          <p className="text-sm text-gray-500">View and manage components for a product version</p>
        </div>
        <Button onClick={() => setShowAdd(true)} disabled={!selectedVersion}><Plus size={16} />Add Component</Button>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-2 gap-4">
          <Select label="Product" value={selectedProduct} onChange={e => loadVersions(e.target.value)}>
            <option value="">Select product…</option>
            {products.map(p => <option key={p.productId} value={p.productId}>{p.name} ({p.productCode})</option>)}
          </Select>
          <Select label="Version" value={selectedVersion} onChange={e => setSelectedVersion(e.target.value)} disabled={!versions.length}>
            <option value="">Select version…</option>
            {versions.map(v => <option key={v.versionId} value={v.versionId}>v{v.versionNumber} — {v.status}</option>)}
          </Select>
        </div>
      </Card>

      <Card>
        {!selectedVersion ? (
          <p className="text-sm text-gray-400 text-center py-8">Select a product and version to view its BOM</p>
        ) : bomTree.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">No components yet. Click "Add Component" to start building the BOM.</p>
        ) : (
          <div className="p-4 space-y-1">
            {bomTree.map(node => <BomTreeNode key={node.componentId} node={node} depth={0} />)}
          </div>
        )}
      </Card>

      <Modal isOpen={showAdd} onClose={() => setShowAdd(false)} title="Add BOM Component">
        <div className="space-y-4">
          <Input label="Component Code *" placeholder="e.g. ENG-001" value={form.componentCode}
            onChange={e => setForm(f => ({ ...f, componentCode: e.target.value }))} />
          <Input label="Name *" placeholder="e.g. Engine Assembly" value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          <Select label="Component Type" value={form.componentType} onChange={e => setForm(f => ({ ...f, componentType: e.target.value }))}>
            <option value="">Select type…</option>
            <option value="MECHANICAL">Mechanical</option>
            <option value="ELECTRICAL">Electrical</option>
            <option value="SOFTWARE">Software</option>
            <option value="RAW_MATERIAL">Raw Material</option>
            <option value="ASSEMBLY">Assembly</option>
          </Select>
          <Select label="Parent Component (optional)" value={form.parentComponentId} onChange={e => setForm(f => ({ ...f, parentComponentId: e.target.value }))}>
            <option value="">None (top-level)</option>
            {allComponents.map(c => <option key={c.componentId} value={c.componentId}>{c.componentCode} — {c.name}</option>)}
          </Select>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Quantity" placeholder="1" value={form.quantity}
              onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))} />
            <Input label="Unit" placeholder="e.g. pcs, kg" value={form.unit}
              onChange={e => setForm(f => ({ ...f, unit: e.target.value }))} />
          </div>
          <Textarea label="Notes" placeholder="Optional notes…" value={form.notes}
            onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowAdd(false)}>Cancel</Button>
            <Button onClick={() => addMutation.mutate({ ...form, parentComponentId: form.parentComponentId || undefined })}
              disabled={!form.componentCode || !form.name || addMutation.isLoading}>
              {addMutation.isLoading ? 'Adding…' : 'Add Component'}
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

function BomTreeNode({ node, depth }) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = node.children && node.children.length > 0;
  return (
    <div>
      <div className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50 transition-colors"
        style={{ marginLeft: `${depth * 24}px` }}>
        {hasChildren ? (
          <button onClick={() => setExpanded(e => !e)} className="text-gray-400 hover:text-gray-600">
            <ChevronRight size={14} className={`transition-transform ${expanded ? 'rotate-90' : ''}`} />
          </button>
        ) : (
          <span className="w-[14px]" />
        )}
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-gray-400">{node.componentCode}</span>
            <span className="text-sm font-medium text-gray-800">{node.name}</span>
            {node.componentType && <Badge status={node.componentType} />}
          </div>
          <p className="text-xs text-gray-400 mt-0.5">
            Qty: {node.quantity} {node.unit || ''}
            {node.notes && ` · ${node.notes}`}
          </p>
        </div>
      </div>
      {hasChildren && expanded && node.children.map(child => (
        <BomTreeNode key={child.componentId} node={child} depth={depth + 1} />
      ))}
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
            <div className="flex-shrink-0 bg-white border-2 border-primary-200 rounded-xl px-5 py-4 text-center min-w-32">
              <div className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold mx-auto mb-2 bg-primary-600 text-white">
                {phase.sequenceOrder}
              </div>
              <p className="text-sm font-semibold text-gray-800">{phase.displayName}</p>
            </div>
            {i < phases.length - 1 && <ArrowRight size={20} className="text-gray-300 flex-shrink-0" />}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}

// ── DOCUMENTS PAGE ────────────────────────────────────────────
export function TdpPage() {
  const qc = useQueryClient();
  const [selectedProduct, setSelectedProduct] = React.useState('');
  const [selectedVersion, setSelectedVersion] = React.useState('');
  const [versions, setVersions] = React.useState([]);
  const [isDragging, setIsDragging] = React.useState(false);
  const [uploading, setUploading] = React.useState(false);
  const [uploadError, setUploadError] = React.useState('');
  const [uploadSuccess, setUploadSuccess] = React.useState('');
  const [docType, setDocType] = React.useState('DRAWING');
  const fileInputRef = React.useRef(null);

  const { data: products = [] } = useQuery('products', plmApi.getProducts);
  const { data: documents = [], isLoading: docsLoading } = useQuery(
    ['documents', selectedVersion],
    () => plmApi.getDocuments(selectedVersion),
    { enabled: !!selectedVersion }
  );

  const loadVersions = async (productId) => {
    setSelectedProduct(productId);
    setSelectedVersion('');
    setVersions([]);
    if (productId) {
      const v = await plmApi.getVersions(productId);
      setVersions(v);
    }
  };

  const handleUpload = async (file) => {
    if (!selectedVersion) { setUploadError('Please select a product version first.'); return; }
    if (!file) return;
    setUploading(true);
    setUploadError('');
    setUploadSuccess('');
    try {
      const formData = new FormData();
      formData.append('versionId', selectedVersion);
      formData.append('documentType', docType);
      formData.append('file', file);
      await plmApi.uploadDocument(formData);
      qc.invalidateQueries(['documents', selectedVersion]);
      setUploadSuccess(`"${file.name}" uploaded successfully.`);
    } catch (err) {
      setUploadError(err.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const onDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleUpload(file);
  };

  const onFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (file) handleUpload(file);
    e.target.value = '';
  };

  const formatSize = (bytes) => {
    if (!bytes) return '—';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const statusColor = (s) => {
    if (s === 'APPROVED') return 'bg-green-100 text-green-700';
    if (s === 'REJECTED') return 'bg-red-100 text-red-700';
    return 'bg-yellow-100 text-yellow-700';
  };

  const docTypeOptions = ['DRAWING', 'SPECIFICATION', 'TEST_REPORT', 'MANUAL', 'CERTIFICATE', 'OTHER'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-gray-900">Technical Data Packages</h2>
        <p className="text-sm text-gray-500">Upload and manage documents attached to product versions</p>
      </div>

      {/* Selectors */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 flex flex-wrap gap-4">
        <div className="flex-1 min-w-40">
          <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">Product</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            value={selectedProduct}
            onChange={e => loadVersions(e.target.value)}
          >
            <option value="">Select product…</option>
            {products.map(p => (
              <option key={p.productId} value={p.productId}>{p.name} ({p.productCode})</option>
            ))}
          </select>
        </div>
        <div className="flex-1 min-w-40">
          <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">Version</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            value={selectedVersion}
            onChange={e => setSelectedVersion(e.target.value)}
            disabled={!selectedProduct}
          >
            <option value="">Select version…</option>
            {versions.map(v => (
              <option key={v.versionId} value={v.versionId}>{v.versionNumber} — {v.status}</option>
            ))}
          </select>
        </div>
        <div className="flex-1 min-w-40">
          <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">Document Type</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            value={docType}
            onChange={e => setDocType(e.target.value)}
          >
            {docTypeOptions.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
          </select>
        </div>
      </div>

      {/* Upload Zone */}
      <div
        className={`border-2 border-dashed rounded-xl p-10 text-center transition-colors cursor-pointer ${
          isDragging ? 'border-primary-500 bg-primary-50' : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
        } ${!selectedVersion ? 'opacity-50 pointer-events-none' : ''}`}
        onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={onDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input ref={fileInputRef} type="file" className="hidden" onChange={onFileSelect} />
        {uploading ? (
          <div className="flex flex-col items-center gap-3">
            <div className="w-10 h-10 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
            <p className="text-sm text-gray-600">Uploading…</p>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3">
            <Upload size={36} className={isDragging ? 'text-primary-500' : 'text-gray-400'} />
            <div>
              <p className="text-sm font-semibold text-gray-700">Drag & drop a file here</p>
              <p className="text-xs text-gray-400 mt-1">or click to browse from your computer</p>
            </div>
            {!selectedVersion && (
              <p className="text-xs text-amber-600 font-medium">Select a product version above first</p>
            )}
          </div>
        )}
      </div>

      {uploadError && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">{uploadError}</div>
      )}
      {uploadSuccess && (
        <div className="bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg px-4 py-3">{uploadSuccess}</div>
      )}

      {/* Document List */}
      {selectedVersion && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-800">Uploaded Documents</h3>
          </div>
          {docsLoading ? (
            <div className="p-8 text-center text-sm text-gray-400">Loading documents…</div>
          ) : documents.length === 0 ? (
            <div className="p-8 text-center text-sm text-gray-400">No documents uploaded yet for this version.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-xs text-gray-500 uppercase tracking-wide">
                <tr>
                  <th className="px-5 py-3 text-left">File Name</th>
                  <th className="px-5 py-3 text-left">Type</th>
                  <th className="px-5 py-3 text-left">Size</th>
                  <th className="px-5 py-3 text-left">Status</th>
                  <th className="px-5 py-3 text-left">Uploaded</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {documents.map(doc => (
                  <tr key={doc.tdpId} className="hover:bg-gray-50">
                    <td className="px-5 py-3">
                      <a
                        href={doc.storageUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary-600 hover:underline font-medium"
                      >
                        {doc.documentName}
                      </a>
                    </td>
                    <td className="px-5 py-3 text-gray-600">{doc.documentType}</td>
                    <td className="px-5 py-3 text-gray-500">{formatSize(doc.fileSizeBytes)}</td>
                    <td className="px-5 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-semibold ${statusColor(doc.approvalStatus)}`}>
                        {doc.approvalStatus}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-gray-500">
                      {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
