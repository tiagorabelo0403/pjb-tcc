# ADR-0013 — modularização do Process Material Dossier e síntese executiva

## Contexto

`ProcessMaterialDossierService` concentrava construção de entrada, normalização textual, heurísticas de controvérsia, vetores de tese, lacunas probatórias, checklist de protocolo, classificação de prova, classificação negocial e montagem de diagnósticos. A classe ficou funcionalmente útil, mas com responsabilidade ampla demais para um eixo estratégico do kernel advisory.

Além disso, a saída pública do dossiê ainda não trazia uma síntese executiva curta e um foco estratégico imediato em `diagnostics`, o que reduzia a utilidade direta para superfícies processuais e assistentes jurídicos.

## Decisão

A trilha foi reorganizada em colaboradores dedicados:

- `ProcessMaterialDossierInputFactory`
- `ProcessMaterialDossierTextSupport`
- `ProcessMaterialDossierHeuristics`
- `ProcessMaterialDossierDiagnosticsFactory`
- `ProcessMaterialDossierInput`
- `ProcessMaterialDossierAnalysis`

`ProcessMaterialDossierService` passou a atuar como orquestrador curto.

A saída pública do `ProcessMaterialDossierReport` foi preservada, mas `diagnostics` passou a incluir:

- `dossierReadinessScore`
- `attentionBand`
- `executiveSummary`
- `strategicFocus`

O `ProcessoMaterializadoConsumer` também foi alinhado para materializar esses sinais executivos no dossiê simplificado de consulta.

## Consequências

- menor concentração de responsabilidade no serviço principal
- heurísticas reutilizáveis e mais fáceis de testar
- melhor legibilidade para futuras extensões do kernel advisory
- ganho funcional sem quebra do contrato público do report
- superfícies consumidoras passam a receber síntese e foco estratégico sem depender de outra camada de pós-processamento
