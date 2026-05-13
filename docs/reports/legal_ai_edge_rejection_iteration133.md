# Round 133 — legal ai edge rejection

## Escopo
- `/api/ai/legal/minuta`
- `/api/ai/legal/grounding/check`
- `/api/ai/legal/conversation`

## Evidência executável adicionada
- `LegalAiEdgeGovernanceIT`
- `LegalAiEdgePolicyProviderContractTest`
- `PjbLegalAiEdgePolicyConsumer-PjbLegalAiEdgePolicyProvider.json`
- `JuridicaLegalAiEdgeBoundaryRound133ArchitectureTest`
- `PjbLegalAiEdgePolicyContractCoverageArchitectureTest`

## Cenários materiais
- `403 browser_origin_not_allowed` para `Origin` não confiável nas três rotas
- `415 content_type_not_allowed` para `text/plain` nas três rotas, com origem confiável

## Resultado
A borda das superfícies jurídicas passou a ter prova executável para rejeição por origem não atestada e por `Content-Type` fora da política governada.
