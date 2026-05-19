import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { Plus, CheckCircle, XCircle, Search } from 'lucide-react';
import { llmApi, plmApi } from '../api';
import { Card, CardHeader, Table, Badge, Button, Modal, Input, Select, Textarea, StatCard, useToast, LoadingState, Alert } from '../components/common';

// ── USERS PAGE ────────────────────────────────────────────────
export function UsersPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState({ employeeId: '', email: '', fullName: '', role: 'ENGINEER', department: '', competencyLevel: 'JUNIOR' });

  const { data: users = [], isLoading } = useQuery('users', () => llmApi.getUsers());
  const { data: userCerts = [] } = useQuery(['certs', selected?.userId], () => llmApi.getUserCerts(selected.userId), { enabled: !!selected });

  const createMutation = useMutation(llmApi.createUser, {
    onSuccess: () => { qc.invalidateQueries('users'); setShowCreate(false); setForm({ employeeId: '', email: '', fullName: '', role: 'ENGINEER', department: '', competencyLevel: 'JUNIOR' }); toast('User created — event sent to PLM & QLM', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  const columns = [
    { key: 'employeeId', label: 'Employee ID' },
    { key: 'fullName', label: 'Name' },
    { key: 'role', label: 'Role', render: r => <Badge status={r.role} /> },
    { key: 'department', label: 'Dept', render: r => r.department || '—' },
    { key: 'competencyLevel', label: 'Level', render: r => <Badge status={r.competencyLevel} /> },
    { key: 'active', label: 'Status', render: r => r.active ? <Badge status="ACTIVE" /> : <Badge status="INACTIVE" /> },
  ];

  const certColumns = [
    { key: 'certNumber', label: 'Cert #' },
    { key: 'courseCode', label: 'Course' },
    { key: 'score', label: 'Score', render: r => `${r.score}%` },
    { key: 'status', label: 'Status', render: r => <Badge status={r.status} /> },
    { key: 'expiresAt', label: 'Expires', render: r => r.expiresAt ? new Date(r.expiresAt).toLocaleDateString() : '—' },
  ];

  if (isLoading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Users</h2>
          <p className="text-sm text-gray-500">{users.length} users — LLM is source of truth</p>
        </div>
        <Button onClick={() => setShowCreate(true)}><Plus size={16} />New User</Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <Table columns={columns} data={users} onRowClick={setSelected} emptyMessage="No users yet." />
        </Card>

        {selected && (
          <Card>
            <CardHeader title={selected.fullName} subtitle={`${selected.employeeId} · ${selected.email}`}
              action={<Badge status={selected.role} />} />
            <div className="p-4 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-gray-50 rounded-xl p-3">
                  <p className="text-xs text-gray-500">Department</p>
                  <p className="text-sm font-semibold mt-1">{selected.department || '—'}</p>
                </div>
                <div className="bg-gray-50 rounded-xl p-3">
                  <p className="text-xs text-gray-500">Level</p>
                  <p className="text-sm font-semibold mt-1">{selected.competencyLevel}</p>
                </div>
              </div>

              <div>
                <p className="text-sm font-semibold text-gray-700 mb-2">Certifications ({userCerts.length})</p>
                {userCerts.length > 0
                  ? <Table columns={certColumns} data={userCerts} />
                  : <p className="text-sm text-gray-400 text-center py-4">No certifications yet</p>}
              </div>
            </div>
          </Card>
        )}
      </div>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Create User" size="lg">
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label="Employee ID *" placeholder="EMP-001" value={form.employeeId} onChange={e => setForm(f => ({ ...f, employeeId: e.target.value }))} />
            <Input label="Email *" type="email" placeholder="user@company.com" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
          </div>
          <Input label="Full Name *" value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
          <div className="grid grid-cols-2 gap-4">
            <Select label="Role *" value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
              <option value="ENGINEER">Engineer</option>
              <option value="QUALITY_MANAGER">Quality Manager</option>
              <option value="TRAINER">Trainer</option>
              <option value="ADMIN">Admin</option>
            </Select>
            <Select label="Level" value={form.competencyLevel} onChange={e => setForm(f => ({ ...f, competencyLevel: e.target.value }))}>
              <option value="JUNIOR">Junior</option>
              <option value="MID">Mid</option>
              <option value="SENIOR">Senior</option>
            </Select>
          </div>
          <Input label="Department" placeholder="Engineering" value={form.department} onChange={e => setForm(f => ({ ...f, department: e.target.value }))} />
          <Alert type="info" message="Creating a user publishes user.profile_updated to PLM and QLM, which cache a shadow copy." />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={() => createMutation.mutate(form)} disabled={!form.employeeId || !form.email || !form.fullName || createMutation.isLoading}>
              {createMutation.isLoading ? 'Creating…' : 'Create User'}
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}

// ── PHASE READINESS PAGE ──────────────────────────────────────
export function PhaseReadinessPage() {
  const [phase, setPhase] = useState('');
  const [searched, setSearched] = useState(false);

  const { data: phases = [] } = useQuery('phases', plmApi.getPhases, { retry: 1 });
  const { data: readiness, isLoading, refetch } = useQuery(
    ['phase-readiness', phase], () => llmApi.getPhaseReadiness(phase),
    { enabled: false, retry: 1 }
  );

  const check = () => { setSearched(true); refetch(); };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Phase Readiness</h2>
        <p className="text-sm text-gray-500">Check if staff are certified for a lifecycle phase before advancing</p>
      </div>

      <Card className="p-6">
        <div className="flex gap-4 items-end">
          <div className="flex-1">
            <Select label="Lifecycle Phase" value={phase} onChange={e => setPhase(e.target.value)}>
              <option value="">Select a phase…</option>
              {phases.map(p => <option key={p.phaseName} value={p.phaseName}>{p.displayName}</option>)}
            </Select>
          </div>
          <Button onClick={check} disabled={!phase || isLoading}>
            <Search size={16} />{isLoading ? 'Checking…' : 'Check Readiness'}
          </Button>
        </div>
      </Card>

      {searched && readiness && (
        <div className="space-y-4">
          <div className={`flex items-center gap-4 p-5 rounded-2xl border-2 ${readiness.ready ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
            {readiness.ready
              ? <CheckCircle size={32} className="text-green-600 flex-shrink-0" />
              : <XCircle size={32} className="text-red-600 flex-shrink-0" />}
            <div>
              <p className={`text-lg font-bold ${readiness.ready ? 'text-green-800' : 'text-red-800'}`}>
                {readiness.ready ? 'Phase Gate: PASS' : 'Phase Gate: BLOCKED'}
              </p>
              <p className={`text-sm ${readiness.ready ? 'text-green-600' : 'text-red-600'}`}>
                {readiness.ready
                  ? `All staff certified for ${phase}`
                  : `${readiness.uncertifiedCount} user(s) missing certification for ${phase}`}
              </p>
            </div>
          </div>

          {!readiness.ready && readiness.uncertifiedUsers?.length > 0 && (
            <Card>
              <CardHeader title="Uncertified Users" subtitle="These users need certification before phase can advance" />
              <div className="p-4 space-y-2">
                {readiness.uncertifiedUsers.map(u => (
                  <div key={u.userId} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                    <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
                      <span className="text-red-600 text-xs font-bold">{u.fullName?.[0]}</span>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-800">{u.fullName}</p>
                      <p className="text-xs text-gray-400">{u.employeeId}</p>
                    </div>
                    <XCircle size={16} className="ml-auto text-red-400" />
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}

// ── ENROLLMENTS PAGE ──────────────────────────────────────────
export function EnrollmentsPage() {
  const { toast, ToastContainer } = useToast();
  const qc = useQueryClient();
  const [showEnroll, setShowEnroll] = useState(false);
  const [form, setForm] = useState({ userId: '', courseId: '' });

  const { data: users = [] } = useQuery('users', () => llmApi.getUsers());

  const enrollMutation = useMutation(llmApi.enroll, {
    onSuccess: () => { setShowEnroll(false); toast('Enrolled successfully', 'success'); },
    onError: e => toast(e.message, 'error'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Enrollments</h2>
          <p className="text-sm text-gray-500">Manual training enrollment (also auto-triggered by events)</p>
        </div>
        <Button onClick={() => setShowEnroll(true)}><Plus size={16} />Enroll User</Button>
      </div>

      <Card className="p-6">
        <Alert type="info" message="Enrollments are also auto-triggered by Pub/Sub events: phase transitions, NCRs, and audit findings automatically enroll relevant staff in required training." />
      </Card>

      <Modal isOpen={showEnroll} onClose={() => setShowEnroll(false)} title="Manual Enrollment">
        <div className="space-y-4">
          <Select label="User *" value={form.userId} onChange={e => setForm(f => ({ ...f, userId: e.target.value }))}>
            <option value="">Select user…</option>
            {users.map(u => <option key={u.userId} value={u.userId}>{u.fullName} ({u.employeeId})</option>)}
          </Select>
          <Input label="Course ID *" placeholder="UUID of the course" value={form.courseId}
            onChange={e => setForm(f => ({ ...f, courseId: e.target.value }))} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowEnroll(false)}>Cancel</Button>
            <Button onClick={() => enrollMutation.mutate({ userId: form.userId, courseId: form.courseId, triggerSource: 'MANUAL' })}
              disabled={!form.userId || !form.courseId}>
              Enroll
            </Button>
          </div>
        </div>
      </Modal>
      <ToastContainer />
    </div>
  );
}
