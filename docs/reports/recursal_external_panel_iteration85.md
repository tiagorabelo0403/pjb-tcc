# Round 85 — painel externo operacional recursal

## Objetivo
Transformar os achados dos manuais de painel do advogado em lente recursal viva para expedientes, acervo, citações/intimações, audiências, pendências, escritório, assistentes e substabelecimento.

## Artefatos
- `RecursalExternalOperationsPanelBlueprint`
- `RecursalExternalOperationsPanelTrackFactory`
- novos labels formais de painel externo em `RecursalFormalSectionLabels`

## Resultado
- nova trilha `PAINEL_EXTERNO_OPERACIONAL_RECURSAL`;
- novo passo de playbook `ORQUESTRAR_PAINEL_EXTERNO_OPERACIONAL`;
- conexão entre acervo, intimações, audiências, área de trabalho e pendências recursais no mesmo workspace já existente;
- reaproveitamento de escritório, assistentes e substabelecimento sem abrir cockpit paralelo.
