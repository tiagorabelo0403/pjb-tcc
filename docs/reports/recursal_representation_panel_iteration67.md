# Round 67 — painel recursal dedicado para partes e representantes

## Objetivo

Organizar a subida recursal para:

- cidadão autor e réu;
- advogado/escritório;
- Defensoria;
- Procuradoria;
- Ministério Público e demais envolvidos institucionais.

O foco foi abrir uma lente recursal dedicada, mas reaproveitando os workspaces e painéis já existentes.

## Artefatos criados

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalRepresentationPanelBlueprint.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalRepresentationPanelTrackFactory.java`

## Artefatos alterados

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalFormalSectionLabels.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalWorkbenchSurfaceCatalog.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationPlaybookService.java`
- `pjb-api/src/test/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceServiceTest.java`
- `pjb-api/src/test/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationServiceTest.java`
- `README.md`

## Superfícies reaproveitadas

- `/api/v1/public/consultas-publicas/workspace`
- `/api/v1/public/consultas-publicas/processos/{numero}`
- `/api/v1/processual/processos/{processoId}/participacao-ativa/workspace`
- `/api/v1/processual/processos/{processoId}/participacao-ativa/submissoes`
- `/api/v1/ui/offices/workspace/executive-dashboard`
- `/api/v1/ui/offices/workspace/processes/{processoId}/access`
- `/api/v1/ui/offices/workspace/processes/{processoId}/reading-mode`
- `/api/v1/ui/professional/workspace/executive-dashboard`
- `/api/v1/ui/professional/workspace/defensoria-executive-dashboard`
- `/api/v1/ui/professional/workspace/defensoria-organ-dashboard`
- `/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard`
- `/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard`
- `/api/v1/institucional/workbench`
- `institutional-support/{branchCode}/coverage`
- `institutional-support/{branchCode}/processos/{processoId}/pre-pauta`

## Resultado estrutural

A escada de visibilidade ganhou um degrau profissional explícito:

- autor/réu enxergam o movimento recursal primeiro;
- depois entram advogado/escritório, Defensoria, Procuradoria ou Ministério Público;
- o acervo mostrado a esses representantes passa a ser uma lente dedicada a recurso ou embargos, já filtrada por ramo/rito, classe recursal e espécie.
