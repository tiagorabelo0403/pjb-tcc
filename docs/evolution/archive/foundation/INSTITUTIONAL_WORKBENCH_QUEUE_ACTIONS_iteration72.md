# Round 72 — quick actions, fila operacional e explainability institucional

Entrou um núcleo único de projeção operacional institucional sem duplicação de regra material.

## Novos endpoints

- `GET /api/v1/institucional/workbench/quick-actions`
- `GET /api/v1/institucional/workbench/operational-queue`

## Núcleo novo

- `InstitutionalWorkbenchProjectionService`
- `InstitutionalWorkbenchController`

## DTOs novos

- `InstitutionalWorkbenchActionResponse`
- `InstitutionalWorkbenchExplainabilityResponse`
- `InstitutionalWorkbenchQuickActionsResponse`
- `InstitutionalWorkbenchQueueItemResponse`
- `InstitutionalWorkbenchOperationalQueueResponse`

## O que passou a acontecer

- quick actions nascem do perfil institucional do usuário e da mesma decisão central do `InstitutionalMaterialActionGuardService`
- fila operacional usa inbox híbrida já existente e não duplica pipeline
- cada item da fila agora pode carregar ação principal, ações permitidas, ações bloqueadas e explainability central
- frontend pode renderizar CTA, bloqueio e redirecionamento sem reimplementar regra material

## Carreiras cobertas

- delegado estadual e federal
- Ministério Público estadual, eleitoral, trabalhista e federal
- Defensoria estadual e federal
- procuradoria municipal, estadual e federal

## Observação estrutural

A nova projeção não cria nova malha de competência. Ela apenas consome a decisão do guard material já centralizado.
