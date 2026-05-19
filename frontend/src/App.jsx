import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import Layout from './components/common/Layout';
import Dashboard from './pages/Dashboard';
import { ProductsPage, ChangeRequestsPage, PhasesPage } from './pages/PlmPages';
import { NcrPage, CapaPage, RiskPage, AuditPage, DocumentsPage } from './pages/QlmPages';
import { UsersPage, PhaseReadinessPage, EnrollmentsPage } from './pages/LlmPages';
import AnalyticsPage from './pages/AnalyticsPage';

import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: 1 },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            {/* PLM */}
            <Route path="/plm" element={<Navigate to="/plm/products" replace />} />
            <Route path="/plm/products" element={<ProductsPage />} />
            <Route path="/plm/versions" element={<ProductsPage />} />
            <Route path="/plm/change-requests" element={<ChangeRequestsPage />} />
            <Route path="/plm/phases" element={<PhasesPage />} />
            {/* QLM */}
            <Route path="/qlm" element={<Navigate to="/qlm/ncr" replace />} />
            <Route path="/qlm/ncr" element={<NcrPage />} />
            <Route path="/qlm/capa" element={<CapaPage />} />
            <Route path="/qlm/audits" element={<AuditPage />} />
            <Route path="/qlm/risks" element={<RiskPage />} />
            <Route path="/qlm/documents" element={<DocumentsPage />} />
            {/* LLM */}
            <Route path="/llm" element={<Navigate to="/llm/users" replace />} />
            <Route path="/llm/users" element={<UsersPage />} />
            <Route path="/llm/enrollments" element={<EnrollmentsPage />} />
            <Route path="/llm/phase-readiness" element={<PhaseReadinessPage />} />
            {/* Analytics */}
            <Route path="/analytics" element={<AnalyticsPage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
