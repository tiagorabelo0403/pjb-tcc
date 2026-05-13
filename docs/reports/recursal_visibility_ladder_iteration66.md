# Round 66 — Escada de visibilidade operacional por degraus

## Objetivo

Transformar a subida recursal em uma escada de visibilidade conectada a superfícies já existentes para:

- autor e réu;
- advogado/escritório;
- Defensoria;
- apoio institucional;
- magistratura de origem;
- magistratura de destino;
- corte superior, quando houver subida estrita.

## Artefatos criados

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalVisibilityLadderBlueprint.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalVisibilityLadderTrackFactory.java`

## Artefatos alterados

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalWorkbenchSurfaceCatalog.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalFormalSectionLabels.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationPlaybookService.java`
- `pjb-api/src/test/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceServiceTest.java`
- `pjb-api/src/test/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationServiceTest.java`

## Superfícies reaproveitadas

- `/api/v1/public/consultas-publicas/workspace`
- `/api/v1/public/consultas-publicas/processos/{numero}`
- `/api/v1/public/consultas-publicas/pages/{pageId}`
- `/api/v1/ui/offices/workspace/processes/{processoId}/access`
- `/api/v1/ui/offices/workspace/processes/{processoId}/reading-mode`
- `/api/v1/ui/offices/workspace/main-dashboard`
- `/api/v1/ui/offices/workspace/executive-dashboard`
- `/api/v1/ui/professional/workspace/executive-dashboard`
- `/api/v1/ui/professional/workspace/defensoria-executive-dashboard`
- `/api/v1/ui/professional/workspace/defensoria-organ-dashboard`
- `/api/v1/ui/professional/workspace/magistrature-executive-dashboard`
- `/api/v1/institucional/workbench`
- `institutional-support/{branchCode}/coverage`
- `institutional-support/{branchCode}/processos/{processoId}/pre-pauta`

## Resultado estrutural

A subida recursal deixou de ser vista apenas como handoff entre painéis decisórios e passou a aparecer em degraus coerentes até a última instância possível, preservando:

- vínculo entre origem e destino;
- sigilo por perfil;
- reaproveitamento de superfícies existentes;
- ausência de dashboard recursal paralelo.
