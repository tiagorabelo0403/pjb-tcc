# ADR-0017 — modularização do NationalProceduralRouting

## Contexto

O `NationalProceduralRoutingService` ainda concentrava montagem de payload, gate econômico, metadata executiva e mensagens operacionais no mesmo arquivo. Isso deixava o eixo procedimental com expansão difícil e aumentava o risco de regressão ao tocar em regras de rito, competência, teto econômico e explicabilidade.

## Decisão

Separar o eixo em colaboradores específicos sem quebrar o contrato público do serviço principal:

- `NationalProceduralRoutingPayloadFactory`
- `NationalProceduralEconomicGateFactory`
- `NationalProceduralRoutingMetadataFactory`
- `NationalProceduralRoutingMessages`

O serviço principal permanece como orquestrador do fluxo procedimental, mantendo decisão material, distribuição e fechamento do `ProceduralRoutingReport`, mas deixando de carregar mensagens operacionais e montagem pesada de estruturas auxiliares.

## Consequências

### Positivas

- menor concentração de responsabilidade no serviço principal
- mensagens operacionais retiradas do corpo do serviço e centralizadas em catálogo próprio
- payload de entrada reutilizável para `ProcessoRequest`, `LaianePeticaoAssistRequest` e `Processo`
- gate econômico isolado para evolução futura sem voltar a poluir o fluxo principal
- metadata estratégica preparada em fábrica própria, com advisory, quality, automation, explainability e acceleration confinados em um ponto dedicado

### Custos

- aumento do número de arquivos do eixo procedural
- necessidade de proteger a centralização de mensagens com teste de governança

## Status

Aceito.
