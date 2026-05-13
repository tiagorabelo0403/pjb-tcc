# PJB workload identity plane

Trust domain base:
- spiffe://pjb.jus.br/pjb

Workloads canônicos:
- api
- worker
- scheduler
- db-edge-rw
- db-edge-ro

Objetivos:
- mTLS interno por identidade de workload
- eliminação progressiva de segredo estático entre API, workers e borda de banco
- egress governado por workload
- service account token projetado apenas para workloads autorizados

Superfície institucional:
- /api/v1/institucional/afiliacoes/{affiliationId}/identidade-workload
