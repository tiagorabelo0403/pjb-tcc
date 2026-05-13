# Round 70 — alertas de prazo e notificações recursais

## Objetivo

Reforçar o eixo recursal com avisos de prazo, calendário e notificações conectados ao que o projeto já possui, sem criar scheduler paralelo nem camada satélite fora da governança existente.

## Arquivos principais

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalDeadlineNotificationBlueprint.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalDeadlineNotificationTrackFactory.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalWorkbenchSurfaceCatalog.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalFormalSectionLabels.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationPlaybookService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceService.java`

## Resultado

O recursal agora indica, no playbook e no workspace, como reaproveitar:

- prazo real do processo;
- calendário operacional e painel temporal;
- preview de notificações;
- preferências de notificação;
- intimação multicanal e trilha de ciência.

Os avisos passam a cobrir especialmente:

- prazo-base da interposição;
- preparo e risco de deserção;
- feriado local não comprovado;
- janela de contrarrazões e adesivo;
- pauta, sustentação oral e publicação do acórdão.
