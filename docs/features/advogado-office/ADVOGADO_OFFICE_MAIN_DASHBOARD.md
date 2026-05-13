# ADVOGADO OFFICE MAIN DASHBOARD

## Objetivo

Unificar no frontend app o painel principal do advogado em contexto institucional, cobrindo:

- nome do escritório ativo
- patrono responsável
- membros totais e online
- fila patronal pendente
- transferências pendentes
- petições governadas pendentes
- prazos críticos
- carteira processual colorida
- calculadora judicial
- calendário de prazos
- movimentação em modo leitura

## Endpoint

- `GET /api/v1/frontend/app/offices/workspace/main-dashboard`

## Resposta principal

A resposta devolve:

- `officeSummary`
- `kpis`
- `legalCockpit`
- `onlineTeamMembers`
- `pendingQueueItems`
- `pendingTransfers`
- `criticalDeadlines`
- `pendingPetitions`
- `quickRoutes`
- `blockers`
- `warnings`

## Regras

- afiliado e patrono enxergam o mesmo escritório ativo no workspace
- advogado autônomo pode usar o mesmo painel quando atuar pelo escritório pessoal
- a carteira processual segue o escopo do workspace ativo
- a fila patronal mostra apenas o que é assinável pelo usuário atual
- petições pendentes do workspace usam a trilha governada de operação processual
- prazos críticos vêm do painel do advogado e são enriquecidos com cor do processo quando o processo está visível no workspace

## Blocos ligados

- `OfficeWorkspaceDashboardService`
- `OfficeWorkspaceLegalCockpitService`
- `OfficeSignatureQueueService`
- `OfficeProcessTransferService`
- `AdvogadoDashboardService`
- `AdvOfficeProcessOperationRepository`
- `OfficeWorkspaceMainDashboardService`
