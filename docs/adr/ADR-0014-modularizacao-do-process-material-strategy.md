# ADR-0014 — modularização do ProcessMaterialStrategy e centralização das mensagens operacionais

## Contexto

`ProcessMaterialStrategyService` concentrava construção de entrada, saneamento textual, score, classificação estratégica e mensagens operacionais em uma única classe. Isso dificultava manutenção, ampliava risco de regressão e contrariava a diretriz de remover mensagens verdes soltas do corpo do serviço.

## Decisão

A trilha `core/kernel/advisory` passa a separar o eixo estratégico em colaboradores próprios:

- `ProcessMaterialStrategyInputFactory`
- `ProcessMaterialStrategyTextSupport`
- `ProcessMaterialStrategyScoringPolicy`
- `ProcessMaterialStrategyReportFactory`
- `ProcessMaterialStrategyControlPointFactory`
- `ProcessMaterialStrategyMetricsFactory`
- `ProcessMaterialStrategyMessages`

`ProcessMaterialStrategyService` permanece apenas como orquestrador.

## Consequências

- mensagens operacionais deixaram de poluir a classe principal e passaram a ter catálogo próprio
- classificação, montagem de listas e métricas ficaram isoladas por responsabilidade
- a trilha ficou mais segura para futuras evoluções, como planner recursal e score executivo de estratégia
- o contrato público de `ProcessMaterialStrategyReport` foi preservado
