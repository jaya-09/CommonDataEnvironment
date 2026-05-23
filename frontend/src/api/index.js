import axios from 'axios';

const api = axios.create({ baseURL: '/api/v1', timeout: 10000 });

api.interceptors.request.use(cfg => {
  cfg.headers['X-Correlation-Id'] = crypto.randomUUID();
  cfg.headers['X-User-Id'] = '00000000-0000-0000-0000-000000000001';
  return cfg;
});

api.interceptors.response.use(
  r => r,
  err => {
    const msg = err.response?.data?.message || err.message || 'Request failed';
    return Promise.reject(new Error(msg));
  }
);

// ── PLM ──────────────────────────────────────────────────────
export const plmApi = {
  getProducts: () => api.get('/plm/products').then(r => r.data),
  getProduct: id => api.get(`/plm/products/${id}`).then(r => r.data),
  createProduct: data => api.post('/plm/products', data).then(r => r.data),
  getVersions: productId => api.get(`/plm/products/${productId}/versions`).then(r => r.data),
  createVersion: (productId, data) => api.post(`/plm/products/${productId}/versions`, data).then(r => r.data),
  getVersion: id => api.get(`/plm/versions/${id}`).then(r => r.data),
  advancePhase: (versionId, data) => api.post(`/plm/versions/${versionId}/phase`, data).then(r => r.data),
  getPhaseGateStatus: (versionId, targetPhase) => api.get(`/plm/versions/${versionId}/phase-gate-status`, { params: { targetPhase } }).then(r => r.data),
  getChangeRequests: versionId => api.get(`/plm/versions/${versionId}/change-requests`).then(r => r.data),
  createChangeRequest: (versionId, data) => api.post(`/plm/versions/${versionId}/change-requests`, data).then(r => r.data),
  submitCR: crId => api.post(`/plm/change-requests/${crId}/submit`).then(r => r.data),
  approveCR: (crId, data) => api.post(`/plm/change-requests/${crId}/approve`, data).then(r => r.data),
  getBom: versionId => api.get(`/plm/versions/${versionId}/bom`).then(r => r.data),
  addBomComponent: (versionId, data) => api.post(`/plm/versions/${versionId}/bom`, data).then(r => r.data),
  getPhases: () => api.get('/plm/phases').then(r => r.data),
  getAuditLog: (type, id) => api.get(`/plm/versions/${id}/audit`).then(r => r.data),
  getPlmUsers: () => api.get('/plm/users').then(r => r.data),
  getDocuments: versionId => api.get(`/plm/documents/${versionId}`).then(r => r.data),
  uploadDocument: formData => api.post('/plm/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data),
};

// ── QLM ──────────────────────────────────────────────────────
export const qlmApi = {
  getNcrs: status => api.get('/qlm/ncr', { params: status ? { status } : {} }).then(r => r.data),
  getNcr: id => api.get(`/qlm/ncr/${id}`).then(r => r.data),
  createNcr: data => api.post('/qlm/ncr', data).then(r => r.data),
  updateNcr: (id, data) => api.put(`/qlm/ncr/${id}`, data).then(r => r.data),
  getCapas: status => api.get('/qlm/capa', { params: status ? { status } : {} }).then(r => r.data),
  createCapa: data => api.post('/qlm/capa', data).then(r => r.data),
  closeCapa: (id, effectiveness) => api.post(`/qlm/capa/${id}/close`, null, { params: { effectivenessCheck: effectiveness } }).then(r => r.data),
  getAudits: status => api.get('/qlm/audits', { params: status ? { status } : {} }).then(r => r.data),
  createAudit: data => api.post('/qlm/audits', data).then(r => r.data),
  completeAudit: (id, params) => api.post(`/qlm/audits/${id}/complete`, null, { params }).then(r => r.data),
  getRisks: () => api.get('/qlm/risks').then(r => r.data),
  createRisk: data => api.post('/qlm/risks', data).then(r => r.data),
  getDocuments: status => api.get('/qlm/documents', { params: status ? { status } : {} }).then(r => r.data),
  createDocument: data => api.post('/qlm/documents', data).then(r => r.data),
  approveDocument: id => api.post(`/qlm/documents/${id}/approve`).then(r => r.data),
};

// ── LLM ──────────────────────────────────────────────────────
export const llmApi = {
  getUsers: params => api.get('/llm/users', { params }).then(r => r.data),
  getUser: id => api.get(`/llm/users/${id}`).then(r => r.data),
  createUser: data => api.post('/llm/users', data).then(r => r.data),
  updateUser: (id, data) => api.put(`/llm/users/${id}`, data).then(r => r.data),
  getUserCerts: id => api.get(`/llm/users/${id}/certifications`).then(r => r.data),
  getPhaseReadiness: phase => api.get(`/llm/phase-readiness/${phase}`).then(r => r.data),
  enroll: data => api.post('/llm/enrollments', data).then(r => r.data),
  completeTraining: (enrollmentId, data) => api.post(`/llm/enrollments/${enrollmentId}/complete`, data).then(r => r.data),
};

export default api;
