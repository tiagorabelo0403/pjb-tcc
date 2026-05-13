# OPA/Envoy ext_authz para a borda institucional do PJB

Objetivo: acrescentar uma camada de autorização contextual na borda soberana sem empurrar lógica de política para dentro dos microfluxos da aplicação.

## Papel dessa camada

- usar Envoy/Gateway como ponto de chamada da autorização externa;
- entregar ao motor de política contexto HTTP, identidade do workload, cabeçalhos sintéticos do data plane institucional e escopo de RLS;
- permitir rollout em `dry-run` antes de bloquear tráfego sensível;
- preservar a checagem documental forte no backend como fonte de verdade final.

## Decisões recomendadas

- `BackendTLSPolicy` para TLS entre gateway e backends;
- identidade de workload no trust domain SPIFFE já descrito no overlay soberano;
- OPA com plugin `envoy_ext_authz_grpc` como sidecar/daemon dedicado na borda institucional;
- políticas separadas por família de rota: leitura institucional, atos críticos, integrações externas, fluxos de homologação.

## Ordem prática de adoção

1. habilitar `dry-run` na borda institucional para medir impacto e coletar trilhas;
2. mover primeiro as políticas de negação óbvias: ausência de afiliação, falta de contexto institucional, operação fora de escopo territorial;
3. só depois transformar em bloqueio duro os atos críticos de assinatura, recurso, redistribuição e emissão documental;
4. manter o `InstitutionalDocumentSecurityGateApplicationService` como guarda final de backend.

## Cabeçalhos/contexto úteis para política

- `X-PJB-Affiliation-Id`
- `X-PJB-Nomination-Id`
- `X-PJB-Institutional-Unit-Code`
- `X-PJB-Institutional-Box-Code`
- `X-PJB-Institutional-Data-Plane-Key`
- `X-PJB-RLS-Affiliation`
- `X-PJB-RLS-Unit`
- `X-PJB-RLS-Box`
- `X-PJB-RLS-Read-Only`

## Guardrails

- políticas de borda não substituem política documental nem assinatura forte;
- nenhum segredo estático de workload deve ser distribuído em manifesto;
- toda rota crítica deve continuar auditável pelo ledger institucional.
