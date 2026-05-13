# ADR-0015 — modularização do StrategicCopilot e centralização das mensagens operacionais

## Contexto

`StrategicCopilotService` concentrava leitura de petição assistida, leitura do twin processual, score estratégico, montagem de ações, watchpoints e mensagens operacionais no mesmo arquivo.

Isso dificultava manutenção, ampliava risco de regressão ao evoluir o eixo `core/kernel/advisory` e mantinha textos institucionais soltos no código oficial.

## Decisão

A trilha foi reorganizada em colaboradores próprios:

- `StrategicCopilotRequestReportFactory`
- `StrategicCopilotProcessReportFactory`
- `StrategicCopilotDiagnosticsFactory`
- `StrategicCopilotDraft`
- `StrategicCopilotSupport`
- `StrategicCopilotMessages`

O serviço principal passa a atuar como orquestrador curto, preservando o contrato público de `StrategicCopilotReport`.

As mensagens operacionais e de orientação estratégica deixam o corpo do serviço e passam a viver em catálogo dedicado.

## Consequências

- menor concentração de responsabilidade
- evolução mais segura do eixo advisory
- remoção de mensagens verdes do código principal
- preparação melhor para futuros planners por fase, rito e órgão julgador
