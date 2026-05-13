# Round 69 — malha recursal segmentada por ramo, rito e sigilo

## Objetivo

Organizar a escada recursal por ramo processual sem duplicar painéis já existentes de cidadão, representantes, secretaria e magistratura.

## Artefatos criados

- `RecursalBranchSegmentationBlueprint`
- `RecursalBranchSegmentationTrackFactory`

## Integrações

- passo `SEGMENTAR_POR_RAMO_RITO_SIGILO` no `RecursalAutomationPlaybookService`
- trilha `MALHA_RECURSAL_POR_RAMO_RITO_SIGILO` no `RecursalAutomationWorkspaceService`
- expansão de `RecursalFormalSectionLabels`
- expansão de `RecursalWorkbenchSurfaceCatalog` com superfícies por ramo

## Resultado esperado

- cidadão continua vendo apenas processos próprios, agora com organização por ramo
- advogados, Defensoria, Procuradoria e MP passam a receber filtro explícito por ramo/rito/classe/espécie
- secretaria e magistratura passam a compartilhar a mesma segmentação por ramo
- sigilo e linguagem operacional deixam de ser genéricos
