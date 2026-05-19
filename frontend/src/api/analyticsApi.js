import axios from 'axios';

const api = axios.create({ baseURL: '/api/v1', timeout: 10000 });
api.interceptors.request.use(cfg => {
  cfg.headers['X-Correlation-Id'] = crypto.randomUUID();
  cfg.headers['X-User-Id'] = '00000000-0000-0000-0000-000000000001';
  return cfg;
});
api.interceptors.response.use(r => r, err => {
  const msg = err.response?.data?.message || err.message || 'Request failed';
  return Promise.reject(new Error(msg));
});

export const analyticsApi = {
  getDashboard:        ()               => api.get('/analytics/dashboard').then(r => r.data),
  getProductHealth:    (status)         => api.get('/analytics/product-health', { params: status ? { status } : {} }).then(r => r.data),
  getVersionHealth:    (versionId)      => api.get(`/analytics/product-health/${versionId}`).then(r => r.data),
  getNcrChains:        (status, code)   => api.get('/analytics/ncr-chains', { params: { status, productCode: code } }).then(r => r.data),
  getPhaseGateHistory: (versionId, res) => api.get('/analytics/phase-gate-history', { params: { versionId, result: res } }).then(r => r.data),
  getRiskMatrix:       (productCode)    => api.get('/analytics/risk-matrix', { params: productCode ? { productCode } : {} }).then(r => r.data),
  getTimeline:         (filters)        => api.get('/analytics/timeline', { params: filters }).then(r => r.data),
};

export default api;
