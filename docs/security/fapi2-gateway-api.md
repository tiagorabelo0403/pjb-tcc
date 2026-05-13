# PJB sovereign API edge

O PJB passa a tratar integrações institucionais sensíveis com um perfil de borda orientado a FAPI 2.0, sender-constrained tokens e Gateway API.

Decisões desta rodada:
- FAPI 2.0 Security Profile como baseline para integrações institucionais de alto valor
- message signing quando a família de integração exigir prova forte do payload
- mutual TLS obrigatório na borda institucional
- private_key_jwt e PAR como baseline para clientes confidenciais governados
- BackendTLSPolicy entre gateway e `pjb-api`
- binding de workload identity com SPIFFE/SPIRE

Overlay principal:
- `infra/k8s/overlays/prod-sovereign-fapi-gateway/`

Relação com a malha existente:
- reaproveita `prod-sovereign-spiffe-trust-plane`
- mantém a borda de dados zero-trust já criada
- evita segredo estático entre gateway, backend e borda de banco
